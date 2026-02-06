package org.hkijena.jast.tasks.workloads;

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
@BackendTaskType(typeId = "image-segment-dda-disk-fast")
public class SegmentDDADiskBackendTaskWorkload implements BackendTaskWorkload {

    private static final List<BackendTaskWorkloadDataSlot> INPUTS = Collections.singletonList(JASTDataSlot.Plate.toSlot());
    private static final List<BackendTaskWorkloadDataSlot> OUTPUTS = Collections.singletonList(JASTDataSlot.StripDisk.toSlot());
    private static final List<BackendTaskWorkloadParameterSlot> PARAMETERS = List.of(
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.Boolean, "fastAlgorithm", "Use fast algorithm", "Use a faster algorithm that assumes that the DDA disk is close to the center of the plate", true),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.Number, "minCirc", "Minimum circularity (0-1)", "Minimum circularity of the disk", 0.5),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.Number, "minFeret", "Minimum diameter (mm)", "Minimum diameter in millimeters. A lower than expected value is better.", 2),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.Number, "maxFeret", "Maximum diameter (mm)", "Maximum diameter in millimeters. A higher than expected value is better.", 12),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.Number, "areaScale", "Fast algorithm: Area scale (0-1)", "Scale the plate area down for searching for the disk. Applies only to the fast algorithm.", 0.25)
    );
    private static final Map<String, String> PARAMETER_OVERRIDES = new HashMap<>();

    static {
        PARAMETER_OVERRIDES.put("minCirc", "dc2e186c-881c-42d1-a9b6-78a8f24579ec/exported/circle filter/minCirc");
        PARAMETER_OVERRIDES.put("minFeret", "dc2e186c-881c-42d1-a9b6-78a8f24579ec/exported/circle filter/minFeret");
        PARAMETER_OVERRIDES.put("maxFeret", "dc2e186c-881c-42d1-a9b6-78a8f24579ec/exported/circle filter/maxFeret");
        PARAMETER_OVERRIDES.put("areaScaleX_", "74d9b4c1-2e11-4b37-8766-e5e4f24f837c/scale-x");
        PARAMETER_OVERRIDES.put("areaScaleY_", "74d9b4c1-2e11-4b37-8766-e5e4f24f837c/scale-y");
    }

    private final ImageRepository imageRepository;
    private final FileStorageService fileStorageService;
    private final BackendTaskUtils taskUtils;
    private BackendTaskRegistry registry;

    @Autowired
    public SegmentDDADiskBackendTaskWorkload(ImageRepository imageRepository, FileStorageService fileStorageService, BackendTaskUtils taskUtils) {
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
        return "Auto-detect DDA disk (v1)";
    }

    @Override
    public String getDescription() {
        return "Automatically detects the disk in disk diffusion assays. Applies basic thresholding to find the disk.";
    }

    @Override
    public BackendTaskWorkloadMode getMode() {
        return BackendTaskWorkloadMode.Single;
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
        return "DDA";
    }

    @Override
    public AssayType getAssayTypeRestriction() {
        return AssayType.DDA;
    }

    @Override
    public boolean isOutputsResult() {
        return false;
    }

    @Override
    public ViewMode getViewModeRestriction() {
        return null;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void execute(BackendTaskWorkloadParams params, ProgressInfo progressInfo) throws Throwable {

        final boolean verbose = params.getRuntimeConfig().isVerbose();
        final boolean preferSystemPackages = params.getRuntimeConfig().isPreferSystemPackages();
        final List<SystemPackage> systemPackages = params.getRuntimeConfig().getSystemPackages();

        taskUtils.writeRawImages(params, params.getPayload().getImageIds(), imageRepository, fileStorageService, progressInfo, verbose);
        taskUtils.writeMaskAnnotations(params, params.getPayload().getImageIds(), "plate", imageRepository, fileStorageService, progressInfo, verbose);

        boolean fastAlgorithm = (boolean) params.getPayload().getParameter("fastAlgorithm").getValue();

        Map<String, Object> parameterOverrides = new HashMap<>();
        for (BackendTaskParameterPayload parameter : params.getPayload().getParameters()) {
            String key = PARAMETER_OVERRIDES.get(parameter.getId());
            if(key != null) {
                parameterOverrides.put(key, parameter.getValue());
            }
        }

        if(fastAlgorithm) {
            parameterOverrides.put(PARAMETER_OVERRIDES.get("areaScaleX_"), params.getPayload().getParameter("areaScale").getValue());
            parameterOverrides.put(PARAMETER_OVERRIDES.get("areaScaleY_"), params.getPayload().getParameter("areaScale").getValue());
        }

        Path projectFilePath = taskUtils.writeSharedFile(params, fastAlgorithm ? Path.of("workflows","image-segment-dda-disk-fast.jip") : Path.of("workflows","image-segment-dda-disk.jip"));
        progressInfo.log("Project file is " + projectFilePath);
        taskUtils.runJIPipe(params, projectFilePath, parameterOverrides, "", progressInfo, systemPackages, preferSystemPackages, params.getRuntimeConfig().isVerbose());

        Map<String, Path> maskAnnotationsConfig = new HashMap<>();
        maskAnnotationsConfig.put("strip-disk", params.getTmpPath().resolve("strip-disk"));
        taskUtils.readMaskAnnotations(params.getPayload().getImageIds(), maskAnnotationsConfig, imageRepository, fileStorageService, progressInfo, verbose);
    }
}
