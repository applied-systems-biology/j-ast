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

package org.hkijena.jast.tasks.workloads.etest;

import jakarta.transaction.Transactional;
import org.hkijena.jast.config.SystemPackage;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.ViewMode;
import org.hkijena.jast.payloads.task.BackendTaskParameterPayload;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.services.BackendTaskRegistry;
import org.hkijena.jast.services.BackendTaskUtils;
import org.hkijena.jast.services.FileStorageService;
import org.hkijena.jast.tasks.*;
import org.hkijena.jast.utils.JASTDataSlot;
import org.hkijena.jast.utils.ProgressInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@BackendTaskType(typeId = "etest-copy-registered-zoi-shape")
public class ETestCopyRegisteredZOIShapeWorkload implements BackendTaskWorkload {

    private static final List<BackendTaskWorkloadDataSlot> INPUTS = List.of(JASTDataSlot.StripDisk.toSlot(),
            JASTDataSlot.ZOIShape.toSlot(BackendTaskWorkloadDataSlotValidationMode.OncePerRow));
    private static final List<BackendTaskWorkloadDataSlot> OUTPUTS = Collections.emptyList();
    private static final List<BackendTaskWorkloadParameterSlot> PARAMETERS = Collections.emptyList();
    private static final Map<String, String> PARAMETER_OVERRIDES = new HashMap<>();
    private final ImageRepository imageRepository;
    private final FileStorageService fileStorageService;
    private final BackendTaskUtils taskUtils;
    private BackendTaskRegistry registry;

    @Autowired
    public ETestCopyRegisteredZOIShapeWorkload(ImageRepository imageRepository, FileStorageService fileStorageService, BackendTaskUtils taskUtils) {
        this.imageRepository = imageRepository;
        this.fileStorageService = fileStorageService;
        this.taskUtils = taskUtils;
    }

    @Override
    public void setRegistry(@Lazy BackendTaskRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "Copy and register ZOI shape across timeline";
    }

    @Override
    public String getDescription() {
        return "Copies the first available ZOI shape to the other images in the timeline. " +
                "The strip is used to register the ZOI shape image.";
    }

    @Override
    public BackendTaskWorkloadMode getMode() {
        return BackendTaskWorkloadMode.FullRow;
    }

    @Override
    public List<BackendTaskWorkloadDataSlot> getInputs() {
        return INPUTS;
    }

    @Override
    public List<BackendTaskWorkloadDataSlot> getOutputs() {
        return OUTPUTS;
    }

    @Override
    public List<BackendTaskWorkloadParameterSlot> getParameters() {
        return PARAMETERS;
    }

    @Override
    public String getCategory() {
        return "E-Test";
    }

    @Override
    public AssayType getAssayTypeRestriction() {
        return AssayType.ETest;
    }

    @Override
    public boolean isOutputsResult() {
        return false;
    }

    @Override
    public ViewMode getViewModeRestriction() {
        return ViewMode.Timeline;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void execute(BackendTaskWorkloadParams params, ProgressInfo progressInfo) throws Throwable {

        final boolean verbose = params.getRuntimeConfig().isVerbose();
        final boolean preferSystemPackages = params.getRuntimeConfig().isPreferSystemPackages();
        final List<SystemPackage> systemPackages = params.getRuntimeConfig().getSystemPackages();

        taskUtils.writeRawImages(params, params.getPayload().getImageIds(), imageRepository, fileStorageService, progressInfo, verbose);
        taskUtils.writeMetadata(params, params.getPayload().getImageIds(), imageRepository, progressInfo, verbose);
        taskUtils.writeMaskAnnotations(params, params.getPayload().getImageIds(), "strip-disk", imageRepository, fileStorageService, progressInfo, verbose);
        taskUtils.writeRowFirstMaskAnnotations(params, params.getPayload().getImageIds(), "zoi-shape", imageRepository, fileStorageService, progressInfo, verbose);

        Map<String, Object> parameterOverrides = new HashMap<>();
        for (BackendTaskParameterPayload parameter : params.getPayload().getParameters()) {
            parameterOverrides.put(PARAMETER_OVERRIDES.get(parameter.getId()), parameter.getValue());
        }

        Path projectFilePath = taskUtils.writeSharedFile(params, Path.of("workflows", "etest-copy-registered-zoi-shape.jip"));
        progressInfo.log("Project file is " + projectFilePath);
        taskUtils.runJIPipe(params, projectFilePath, parameterOverrides, "", progressInfo, systemPackages, preferSystemPackages, verbose);

        Map<String, Path> maskAnnotationsConfig = new HashMap<>();
        maskAnnotationsConfig.put("zoi-shape", params.getTmpPath().resolve("zoi-shape-aligned"));
        taskUtils.readMaskAnnotations(params.getPayload().getImageIds(), maskAnnotationsConfig, imageRepository, fileStorageService, progressInfo, verbose);
    }
}
