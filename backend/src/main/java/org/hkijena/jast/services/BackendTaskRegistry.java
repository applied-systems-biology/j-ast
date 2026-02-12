package org.hkijena.jast.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.hkijena.jast.config.RuntimeConfig;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.ViewMode;
import org.hkijena.jast.tasks.*;
import org.hkijena.jast.tasks.workloads.JIPipePluginBackendTaskWorkload;
import org.hkijena.jast.utils.JASTDataSlot;
import org.hkijena.jast.utils.JsonUtils;
import org.hkijena.jast.utils.PathUtils;
import org.hkijena.jast.utils.StringUtils;
import org.jsoup.Jsoup;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class BackendTaskRegistry {
    private final RuntimeConfig runtimeConfig;
    private final ApplicationContext applicationContext;
    private final JIPipePluginBackendTaskWorkloadFactory  taskWorkloadFactory;
    private final Map<String, BackendTaskWorkload> registeredTasks = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger(BackendTaskRegistry.class.getName());

    @Autowired
    public BackendTaskRegistry(RuntimeConfig runtimeConfig, ApplicationContext applicationContext, JIPipePluginBackendTaskWorkloadFactory taskWorkloadFactory) {
        this.runtimeConfig = runtimeConfig;
        this.applicationContext = applicationContext;
        this.taskWorkloadFactory = taskWorkloadFactory;
        initialize();
    }

    private void initialize() {
        Reflections reflections = new Reflections("org.hkijena.jast");
        Set<Class<?>> taskClasses = reflections.getTypesAnnotatedWith(BackendTaskType.class);
        for (Class<?> taskClass : taskClasses) {
            if (BackendTaskWorkload.class.isAssignableFrom(taskClass)) {
                registerBackendTaskFromClass(taskClass);
            }
        }

        Path pluginsDir = Paths.get(runtimeConfig.getSharedResourcesDirectory()).resolve("plugins");
        logger.info("Looking for plugins in " + pluginsDir);

        if (Files.isDirectory(pluginsDir)) {
            try {
                for (Path pluginFile : PathUtils.listFiles(pluginsDir)) {
                    try {
                        if (pluginFile.getFileName().toString().endsWith(".jip")) {
                            logger.info("Attempting to read plugin from file " + pluginFile);
                            registerBackendTaskFromJIPipeFile(pluginFile);
                        }
                    } catch (Exception e) {
                        logger.error("Could not read plugin file " + pluginFile, e);
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void registerBackendTaskFromJIPipeFile(Path pluginFile) {
        final String id = "plugin-" + pluginFile.getFileName().toString().toLowerCase(Locale.ROOT).replace(".jip", "");
        JsonNode rootNode = JsonUtils.readFromFile(pluginFile, JsonNode.class);

        // We search for the tmp_dir user directory
        boolean foundTmpPathUserDirectory = false;
        for (JsonNode jsonNode : ImmutableList.copyOf(rootNode.path("metadata").path("user-directories").path("user-directories").path("items").elements())) {
            if (jsonNode.path("parameters").path("key").path("value").asText().equals("tmp_dir")) {
                foundTmpPathUserDirectory = true;
                break;
            }
        }

        // jast_plugin must be true
        if (!readGlobalBooleanParameterFromJIPipeWorkflow(rootNode, "jast_plugin", false)) {
            logger.info("Plugin " + id + " is disabled (jast_plugin not set to true)");
            return;
        }

        String name = rootNode.path("metadata").path("name").asText();
        String description = Jsoup.parse(rootNode.path("metadata").path("description").asText("<html></html>")).text();
        String category = readGlobalStringParameterFromJIPipeWorkflow(rootNode, "jast_plugin_category", "");
        BackendTaskWorkloadMode mode = readGlobalEnumParameterFromJIPipeWorkflow(rootNode, "jast_plugin_mode", BackendTaskWorkloadMode.Single, BackendTaskWorkloadMode.class);
        boolean generatesResults = readGlobalBooleanParameterFromJIPipeWorkflow(rootNode, "jast_plugin_generates_results", false);
        AssayType assayTypeRestriction = readGlobalEnumParameterFromJIPipeWorkflow(rootNode, "jast_plugin_assay_type_restriction", AssayType.Unknown, AssayType.class);
        ViewMode viewModeRestriction = readGlobalEnumParameterFromJIPipeWorkflow(rootNode, "jast_plugin_view_mode_restriction", null, ViewMode.class);

        List<BackendTaskWorkloadDataSlot> inputs = new ArrayList<>();
        List<BackendTaskWorkloadDataSlot> outputs = new ArrayList<>();
        List<BackendTaskWorkloadParameterSlot> parameters = new ArrayList<>();

        // Auto-discover inputs and outputs
        for (String slotName : readGlobalStringListParameterFromJIPipeWorkflow(rootNode, "jast_plugin_inputs", Collections.emptyList())) {
            BackendTaskWorkloadDataSlot slot = readSlotName(slotName);
            if(slot != null) {
                inputs.add(slot);
            }
        }
        for (String slotName : readGlobalStringListParameterFromJIPipeWorkflow(rootNode, "jast_plugin_outputs", Collections.emptyList())) {
            BackendTaskWorkloadDataSlot slot = readSlotName(slotName);
            if(slot != null) {
                outputs.add(slot);
            }
        }

        // Inject result parameters
        if (generatesResults) {
            parameters.add(new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.String, BackendTaskWorkloadParameterSlotType.Common, "__jast__result-name", "Result name", "The name of the generated result folder", "DiskImageR-style result (single)"));
            parameters.add(new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.String, BackendTaskWorkloadParameterSlotType.Common, "__jast__result-description", "Result description", "Description of the generated result", "RAD/FoG/ZOI"));
        }

        // Auto-discover global parameters
        for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(rootNode.path("metadata").path("global-parameters").path("parameters").fields())) {
            if (entry.getKey().startsWith("jast_plugin")) {
                continue;
            }
            // For global parameters we can get the exact parameter type from the description
            String typeId = entry.getValue().path("field-class").asText();

            // Extract name & description
            String parameterName = StringUtils.orElse(entry.getValue().path("name").asText(), entry.getKey());
            String parameterDescription = StringUtils.orElse(entry.getValue().path("description").asText(), "");

            // We only support basic primitive parameters
            switch (typeId) {
                case "java.lang.String":
                    parameters.add(new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.String,
                            BackendTaskWorkloadParameterSlotType.Common, "/" + entry.getKey(),
                            parameterName,
                            parameterDescription,
                            entry.getValue().path("value").asText()));
                    break;
                case "java.lang.Double":
                case "primitive.double":
                case "java.lang.Float":
                case "primitive.float":
                case "java.lang.Integer":
                case "primitive.int":
                case "java.lang.Long":
                case "primitive.long":
                case "java.lang.Short":
                case "primitive.short":
                case "java.lang.Byte":
                case "primitive.byte":
                    parameters.add(new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number,
                            BackendTaskWorkloadParameterSlotType.Common, "/" + entry.getKey(),
                            parameterName,
                            parameterDescription,
                            entry.getValue().path("value").asDouble()));
                    break;
                case "java.lang.Boolean":
                case "primitive.boolean":
                    parameters.add(new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Boolean,
                            BackendTaskWorkloadParameterSlotType.Common, "/" + entry.getKey(),
                            parameterName,
                            parameterDescription,
                            entry.getValue().path("value").asBoolean()));
                    break;
                default:
                    logger.warn("Unsupported parameter type '" + typeId + "' for parameter '" + entry.getKey() + "'");
            }
        }

        // Auto-discover referenced parameters (not fully supported yet due to missing JIPipe functions!)
        JsonNode referencedParametersNode = rootNode.path("additional-metadata").path("org.hkijena.jipipe:pipeline-parameters");
        if (!referencedParametersNode.isMissingNode()) {
            for (JsonNode referencedParameterGroupNode : ImmutableList.copyOf(referencedParametersNode.path("exported-parameters").path("parameter-reference-groups").elements())) {
                String groupName = referencedParameterGroupNode.path("name").asText();
                for (JsonNode referencedParameterNode : ImmutableList.copyOf(referencedParameterGroupNode.path("content").elements())) {
                    String path = referencedParameterNode.path("path").asText();
                    String parameterName = referencedParameterNode.path("custom-name").asText();
                    String parameterDescription = referencedParameterNode.path("custom-description").asText();
                    if (StringUtils.isNullOrEmpty(parameterName)) {
                        logger.warn("Referenced parameters should not have an empty custom name!");
                        parameterName = "Unnamed";
                    }
                    if (StringUtils.isNullOrEmpty(parameterDescription)) {
                        logger.warn("Referenced parameters should not have an empty custom description!");
                        parameterDescription = "";
                    }

                    // We disassemble the path
                    String nodeUUID = path.split("/")[0];
                    String parameterKey = path.substring(nodeUUID.length() + 1);

                    JsonNode nodeParameterNode = rootNode.path("graph").path("nodes").path(nodeUUID).path(parameterKey);
                    if (nodeParameterNode.isMissingNode()) {
                        logger.error("Unable to find referenced parameter '" + parameterKey + "' in node '" + nodeUUID + "' (" + pluginFile + ")");
                    } else {
                        // We only accept number/boolean/string fields
                        if (nodeParameterNode.isTextual()) {
                            logger.warn("Referenced parameter '" + parameterKey + "' in node '" + nodeUUID + "' (" + pluginFile + ") is determined to be a string. Please note that parameter references are not yet fully supported.");
                            parameters.add(new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.String,
                                    BackendTaskWorkloadParameterSlotType.Common, path,
                                    parameterName,
                                    parameterDescription,
                                    nodeParameterNode.asText()));
                        } else if (nodeParameterNode.isNumber()) {
                            logger.warn("Referenced parameter '" + parameterKey + "' in node '" + nodeUUID + "' (" + pluginFile + ") is determined to be a number. Please note that parameter references are not yet fully supported.");
                            parameters.add(new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number,
                                    BackendTaskWorkloadParameterSlotType.Common, path,
                                    parameterName,
                                    parameterDescription,
                                    nodeParameterNode.asDouble()));
                        } else if (nodeParameterNode.isBoolean()) {
                            logger.warn("Referenced parameter '" + parameterKey + "' in node '" + nodeUUID + "' (" + pluginFile + ") is determined to be a boolean. Please note that parameter references are not yet fully supported.");
                            parameters.add(new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Boolean,
                                    BackendTaskWorkloadParameterSlotType.Common, path,
                                    parameterName,
                                    parameterDescription,
                                    nodeParameterNode.asBoolean()));
                        } else {
                            logger.error("Unable to guess referenced parameter type of '" + parameterKey + "' in node '" + nodeUUID + "' (" + pluginFile + ")");
                        }
                    }

                }
            }

        }


        JIPipePluginBackendTaskWorkload workload = taskWorkloadFactory.create(pluginFile,
                name,
                description,
                category,
                mode,
                inputs,
                outputs,
                parameters,
                assayTypeRestriction,
                viewModeRestriction,
                generatesResults);

        workload.setRegistry(this);
        logger.info("Registering plugin " + id + " as " + name);
        logger.info("-> mode:" + mode + ", assayType:" + assayTypeRestriction + ", viewMode:" + viewModeRestriction + ", results:" + generatesResults);
        for (BackendTaskWorkloadDataSlot input : inputs) {
            logger.info("-> input:" + input);
        }
        for (BackendTaskWorkloadDataSlot output : outputs) {
            logger.info("-> output:" + output);
        }
        for (BackendTaskWorkloadParameterSlot parameter : parameters) {
            logger.info("-> parameter:" + parameter);
        }


        registeredTasks.put(id, workload);
    }

    private BackendTaskWorkloadDataSlot readSlotName(String slotName) {

        // Read out the postfix (! = always, ? = optional, # = once)
        BackendTaskWorkloadDataSlotValidationMode mode = BackendTaskWorkloadDataSlotValidationMode.Always;
        if(slotName.endsWith("!")) {
            mode = BackendTaskWorkloadDataSlotValidationMode.Always;
            slotName = slotName.substring(0, slotName.length() - 1);
        }
        else if(slotName.endsWith("?")) {
            mode = BackendTaskWorkloadDataSlotValidationMode.Optional;
            slotName = slotName.substring(0, slotName.length() - 1);
        }
        else if(slotName.endsWith("#")) {
            mode = BackendTaskWorkloadDataSlotValidationMode.OncePerRow;
            slotName = slotName.substring(0, slotName.length() - 1);
        }

        switch (slotName) {
            case "raw":
                return JASTDataSlot.Raw.toSlot(mode);
            case "plate":
                return JASTDataSlot.Plate.toSlot(mode);
            case "strip-disk":
            case "strip":
            case "disk":
                return JASTDataSlot.StripDisk.toSlot(mode);
            case "zoi-shape":
                return JASTDataSlot.ZOIShape.toSlot(mode);
            case "pixelSize":
                // Info: should never be an input!
                return JASTDataSlot.PixelSize.toSlot(mode);
        }

        logger.error("Unable to parse slot name '" + slotName + "'");

        return null;
    }

    private List<String> readGlobalStringListParameterFromJIPipeWorkflow(JsonNode rootNode, String key, List<String> defaultValue) {
        JsonNode valueNode = rootNode.path("metadata").path("global-parameters").path("parameters").path(key).path("value");
        if (valueNode.isMissingNode()) {
            return defaultValue;
        }
        try {
            return JsonUtils.getObjectMapper().readerForListOf(String.class).readValue(valueNode);
        } catch (IOException e) {
            logger.error("Could not read global string list parameter " + key + " from JIPipe workflow", e);
            return defaultValue;
        }
    }

    private String readGlobalStringParameterFromJIPipeWorkflow(JsonNode rootNode, String key, String defaultValue) {
        return rootNode.path("metadata").path("global-parameters").path("parameters").path(key).path("value").asText(defaultValue);
    }

    private boolean readGlobalBooleanParameterFromJIPipeWorkflow(JsonNode rootNode, String key, boolean defaultValue) {
        return rootNode.path("metadata").path("global-parameters").path("parameters").path(key).path("value").asBoolean(defaultValue);
    }

    private <T extends Enum<T>> T readGlobalEnumParameterFromJIPipeWorkflow(JsonNode rootNode, String key, T defaultValue, Class<T> enumClass) {
        JsonNode valueNode = rootNode.path("metadata").path("global-parameters").path("parameters").path(key).path("value");
        if (valueNode.isMissingNode() || !valueNode.isTextual()) {
            return defaultValue;
        }

        String textValue = valueNode.asText();
        try {
            return Enum.valueOf(enumClass, textValue);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid enum value '{}' for key '{}'; using default '{}'", textValue, key, defaultValue);
            return defaultValue;
        }
    }

    private void registerBackendTaskFromClass(Class<?> taskClass) {
        try {
            BackendTaskType annotation = taskClass.getAnnotation(BackendTaskType.class);
            if (annotation != null) {
                BackendTaskWorkload task = (BackendTaskWorkload) applicationContext.getBean(taskClass);
                task.setRegistry(this);
                logger.info("Registering task {} as {}", task.getClass().getSimpleName(), annotation.typeId());
                registeredTasks.put(annotation.typeId(), task);
            }
        } catch (Exception e) {
            logger.error("Unable to register {}: {}", taskClass, e.getMessage());
        }
    }

    public BackendTaskWorkload getTask(String taskName) {
        return registeredTasks.get(taskName);
    }

    public Map<String, BackendTaskWorkload> getRegisteredTasks() {
        return registeredTasks;
    }
}
