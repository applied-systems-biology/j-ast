package org.hkijena.jast.tasks.workloads;

import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.ViewMode;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.payloads.task.BackendTaskParameterPayload;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.repositories.ResultRepository;
import org.hkijena.jast.services.BackendTaskRegistry;
import org.hkijena.jast.services.BackendTaskUtils;
import org.hkijena.jast.services.FileStorageService;
import org.hkijena.jast.tasks.*;
import org.hkijena.jast.utils.ProgressInfo;
import org.hkijena.jast.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class JIPipePluginBackendTaskWorkload implements BackendTaskWorkload {
    private Path pluginFile;
    private String name;
    private String description;
    private String category;
    private BackendTaskWorkloadMode mode;
    private List<BackendTaskWorkloadDataSlot> inputs;
    private List<BackendTaskWorkloadDataSlot> outputs;
    private List<BackendTaskWorkloadParameterSlot> parameters;
    private AssayType assayTypeRestriction;
    private ViewMode viewModeRestriction;
    private boolean outputsResult;

    private final ImageRepository imageRepository;
    private final ProjectRepository projectRepository;
    private final ResultRepository resultRepository;
    private final FileStorageService fileStorageService;
    private final BackendTaskUtils taskUtils;
    private BackendTaskRegistry registry;

    @Autowired
    public JIPipePluginBackendTaskWorkload(ImageRepository imageRepository,
                                           ProjectRepository projectRepository,
                                           ResultRepository resultRepository,
                                           FileStorageService fileStorageService,
                                           BackendTaskUtils taskUtils) {
        this.imageRepository = imageRepository;
        this.projectRepository = projectRepository;
        this.resultRepository = resultRepository;
        this.fileStorageService = fileStorageService;
        this.taskUtils = taskUtils;
    }

    public Path getPluginFile() {
        return pluginFile;
    }

    public void setPluginFile(Path pluginFile) {
        this.pluginFile = pluginFile;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setMode(BackendTaskWorkloadMode mode) {
        this.mode = mode;
    }

    public void setInputs(List<BackendTaskWorkloadDataSlot> inputs) {
        this.inputs = inputs;
    }

    public void setOutputs(List<BackendTaskWorkloadDataSlot> outputs) {
        this.outputs = outputs;
    }

    public void setParameters(List<BackendTaskWorkloadParameterSlot> parameters) {
        this.parameters = parameters;
    }

    public void setAssayTypeRestriction(AssayType assayTypeRestriction) {
        this.assayTypeRestriction = assayTypeRestriction;
    }

    public void setViewModeRestriction(ViewMode viewModeRestriction) {
        this.viewModeRestriction = viewModeRestriction;
    }

    public void setOutputsResult(boolean outputsResult) {
        this.outputsResult = outputsResult;
    }

    @Override
    public void setRegistry(@Lazy BackendTaskRegistry registry) {
        this.registry = registry;
    }

    public BackendTaskRegistry getRegistry() {
        return registry;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public BackendTaskWorkloadMode getMode() {
        return mode;
    }

    @Override
    public List<BackendTaskWorkloadDataSlot> getInputs() {
        return inputs;
    }

    @Override
    public List<BackendTaskWorkloadDataSlot> getOutputs() {
        return outputs;
    }

    @Override
    public List<BackendTaskWorkloadParameterSlot> getParameters() {
        return parameters;
    }

    @Override
    public AssayType getAssayTypeRestriction() {
        return assayTypeRestriction;
    }

    @Override
    public ViewMode getViewModeRestriction() {
        return viewModeRestriction;
    }

    @Override
    public boolean isOutputsResult() {
        return outputsResult;
    }

    @Override
    public void execute(BackendTaskWorkloadParams params, ProgressInfo progressInfo) throws Throwable {
        progressInfo.log("Executing plugin " + pluginFile.getFileName());
        Map<String, Object> parameterOverrides = new HashMap<>();

        taskUtils.writeRawImages(params, params.getPayload().getImageIds(), imageRepository, fileStorageService, progressInfo);
        for (BackendTaskWorkloadDataSlot input : inputs) {
            if(input.getType() == BackendTaskWorkloadDataSlotType.ImageMaskAnnotation) {
                taskUtils.writeMaskAnnotations(params, params.getPayload().getImageIds(), input.getName(), imageRepository, fileStorageService, progressInfo);
            }
        }

        for (BackendTaskParameterPayload parameter : params.getPayload().getParameters()) {
            if(parameter.getId().startsWith("__jast__")) {
               continue;
            }
            parameterOverrides.put(parameter.getId(), parameter.getValue());
        }

        Path projectFile = params.getTmpPath().resolve(pluginFile.getFileName());
        Files.copy(pluginFile, projectFile);
        progressInfo.log("Project file is " + projectFile);
        taskUtils.runJIPipe(params, projectFile, parameterOverrides, "", progressInfo);

        // Collect generated annotations
        Map<String, Path> maskAnnotationsConfig = new HashMap<>();

        for (BackendTaskWorkloadDataSlot output : outputs) {
            if(output.getType() == BackendTaskWorkloadDataSlotType.ImageMaskAnnotation) {
                Path annotationPath;
                if(Files.isDirectory( params.getTmpPath().resolve(output.getName() + "_updated"))) {
                    annotationPath = params.getTmpPath().resolve(output.getName() + "_updated");
                }
                else {
                    annotationPath = params.getTmpPath().resolve(output.getName());
                }
                maskAnnotationsConfig.put(output.getName(), annotationPath);
            }
            else if(output.getType() == BackendTaskWorkloadDataSlotType.Raw) {
                taskUtils.readRawImages(params.getPayload().getImageIds(), params.getTmpPath().resolve("raw_updated"), imageRepository, fileStorageService, progressInfo);
            }
        }

        // Execute annotation reading
        if(!maskAnnotationsConfig.isEmpty()) {
            taskUtils.readMaskAnnotations(params.getPayload().getImageIds(), maskAnnotationsConfig, imageRepository, fileStorageService, progressInfo);
        }

        // Process results if enabled
        if(outputsResult) {
            String resultName = StringUtils.orElse(params.getPayload().getParameter("__jast__result-name").getValue(), "Visualization");
            String resultDescription = StringUtils.nullToEmpty(params.getPayload().getParameter("__jast__result-description").getValue());
            Project project = projectRepository.findById(params.getPayload().getProjectId()).get();

            taskUtils.readResultsDirectory(resultName, resultDescription, params.getTmpPath().resolve("results"), project, projectRepository, fileStorageService, progressInfo);
        }
    }
}
