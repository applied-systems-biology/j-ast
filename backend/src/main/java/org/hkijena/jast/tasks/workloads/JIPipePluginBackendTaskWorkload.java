/*
 * Copyright (c) 2026.
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jast.tasks.workloads;

import com.google.common.base.Predicates;
import org.hkijena.jast.config.SystemPackage;
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
    private final ImageRepository imageRepository;
    private final ProjectRepository projectRepository;
    private final ResultRepository resultRepository;
    private final FileStorageService fileStorageService;
    private final BackendTaskUtils taskUtils;
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

    public BackendTaskRegistry getRegistry() {
        return registry;
    }

    @Override
    public void setRegistry(@Lazy BackendTaskRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public BackendTaskWorkloadMode getMode() {
        return mode;
    }

    public void setMode(BackendTaskWorkloadMode mode) {
        this.mode = mode;
    }

    @Override
    public List<BackendTaskWorkloadDataSlot> getInputs() {
        return inputs;
    }

    public void setInputs(List<BackendTaskWorkloadDataSlot> inputs) {
        this.inputs = inputs;
    }

    @Override
    public List<BackendTaskWorkloadDataSlot> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<BackendTaskWorkloadDataSlot> outputs) {
        this.outputs = outputs;
    }

    @Override
    public List<BackendTaskWorkloadParameterSlot> getParameters() {
        return parameters;
    }

    public void setParameters(List<BackendTaskWorkloadParameterSlot> parameters) {
        this.parameters = parameters;
    }

    @Override
    public AssayType getAssayTypeRestriction() {
        return assayTypeRestriction;
    }

    public void setAssayTypeRestriction(AssayType assayTypeRestriction) {
        this.assayTypeRestriction = assayTypeRestriction;
    }

    @Override
    public ViewMode getViewModeRestriction() {
        return viewModeRestriction;
    }

    public void setViewModeRestriction(ViewMode viewModeRestriction) {
        this.viewModeRestriction = viewModeRestriction;
    }

    @Override
    public boolean isOutputsResult() {
        return outputsResult;
    }

    public void setOutputsResult(boolean outputsResult) {
        this.outputsResult = outputsResult;
    }

    @Override
    public void execute(BackendTaskWorkloadParams params, ProgressInfo progressInfo) throws Throwable {
        final boolean verbose = params.getRuntimeConfig().isVerbose();
        final boolean preferSystemPackages = params.getRuntimeConfig().isPreferSystemPackages();
        final List<SystemPackage> systemPackages = params.getRuntimeConfig().getSystemPackages();

        progressInfo.log("Executing plugin " + pluginFile.getFileName());
        Map<String, Object> parameterOverrides = new HashMap<>();

        taskUtils.writeRawImages(params, params.getPayload().getImageIds(), imageRepository, fileStorageService, progressInfo, verbose);
        taskUtils.writeMetadata(params, params.getPayload().getImageIds(), imageRepository, progressInfo, verbose);
        for (BackendTaskWorkloadDataSlot input : inputs) {
            if (input.getType() == BackendTaskWorkloadDataSlotType.ImageMaskAnnotation) {
                taskUtils.writeMaskAnnotations(params, params.getPayload().getImageIds(), input.getName(), imageRepository, fileStorageService, progressInfo, verbose);
            }
        }

        for (BackendTaskParameterPayload parameter : params.getPayload().getParameters()) {
            if (parameter.getId().startsWith("__jast__")) {
                continue;
            }
            parameterOverrides.put(parameter.getId(), parameter.getValue());
        }

        Path projectFile = params.getTmpPath().resolve(pluginFile.getFileName());
        Files.copy(pluginFile, projectFile);
        progressInfo.log("Project file is " + projectFile);
        taskUtils.runJIPipe(params, projectFile, parameterOverrides, "", progressInfo, systemPackages, preferSystemPackages, verbose);

        // Collect generated annotations
        Map<String, Path> maskAnnotationsConfig = new HashMap<>();

        for (BackendTaskWorkloadDataSlot output : outputs) {
            if (output.getType() == BackendTaskWorkloadDataSlotType.ImageMaskAnnotation) {
                Path annotationPath;
                if (Files.isDirectory(params.getTmpPath().resolve(output.getName() + "_updated"))) {
                    annotationPath = params.getTmpPath().resolve(output.getName() + "_updated");
                } else {
                    annotationPath = params.getTmpPath().resolve(output.getName());
                }
                maskAnnotationsConfig.put(output.getName(), annotationPath);
            } else if (output.getType() == BackendTaskWorkloadDataSlotType.Raw) {
                taskUtils.readRawImages(params.getPayload().getImageIds(), params.getTmpPath().resolve("raw_updated"), imageRepository, fileStorageService, progressInfo, verbose);
            }
        }

        // Execute annotation reading
        if (!maskAnnotationsConfig.isEmpty()) {
            taskUtils.readMaskAnnotations(params.getPayload().getImageIds(), maskAnnotationsConfig, imageRepository, fileStorageService, progressInfo, verbose);
        }

        // Process results if enabled
        if (outputsResult) {
            String resultName = StringUtils.orElse(params.getPayload().getParameter("__jast__result-name").getValue(), "Visualization");
            String resultDescription = StringUtils.nullToEmpty(params.getPayload().getParameter("__jast__result-description").getValue());
            Project project = projectRepository.findById(params.getPayload().getProjectId()).get();

            taskUtils.readResultsDirectory(resultName, resultDescription, params.getTmpPath().resolve("results"), project, projectRepository, fileStorageService, Predicates.alwaysTrue(), progressInfo, verbose);
        }
    }
}
