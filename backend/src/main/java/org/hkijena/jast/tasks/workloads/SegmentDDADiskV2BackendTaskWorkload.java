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
@BackendTaskType(typeId = "image-segment-dda-disk-v2")
public class SegmentDDADiskV2BackendTaskWorkload implements BackendTaskWorkload {

    private static final List<BackendTaskWorkloadDataSlot> INPUTS = Collections.singletonList(JASTDataSlot.Plate.toSlot());
    private static final List<BackendTaskWorkloadDataSlot> OUTPUTS = Collections.singletonList(JASTDataSlot.StripDisk.toSlot());
    private static final List<BackendTaskWorkloadParameterSlot> PARAMETERS = List.of(
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.Number, "expectedDiameter", "Expected diameter (mm)", "Expected disk diameter in millimeters", 6),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.Number, "localVarianceDivisor", "Local variance divisor", "Determines the kernel size of the local variance operation. The expected diameter is divided by that value.", 6)
    );
    private static final Map<String, String> PARAMETER_OVERRIDES = new HashMap<>();

    static {
        PARAMETER_OVERRIDES.put("expectedDiameter", "/expectedDiskDiameter");
    }

    private final ImageRepository imageRepository;
    private final FileStorageService fileStorageService;
    private final BackendTaskUtils taskUtils;
    private BackendTaskRegistry registry;

    @Autowired
    public SegmentDDADiskV2BackendTaskWorkload(ImageRepository imageRepository, FileStorageService fileStorageService, BackendTaskUtils taskUtils) {
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
        return "Auto-detect DDA disk (v2)";
    }

    @Override
    public String getDescription() {
        return "Automatically detects the disk in disk diffusion assays. Finds low-variance regions to detect the disk.";
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

        Map<String, Object> parameterOverrides = new HashMap<>();
        for (BackendTaskParameterPayload parameter : params.getPayload().getParameters()) {
            String key = PARAMETER_OVERRIDES.get(parameter.getId());
            if(key != null) {
                parameterOverrides.put(key, parameter.getValue());
            }
        }

        Path projectFilePath = taskUtils.writeSharedFile(params, Path.of("workflows","image-segment-dda-disk-v2.jip"));
        progressInfo.log("Project file is " + projectFilePath);
        taskUtils.runJIPipe(params, projectFilePath, parameterOverrides, "", progressInfo, systemPackages, preferSystemPackages, verbose);

        Map<String, Path> maskAnnotationsConfig = new HashMap<>();
        maskAnnotationsConfig.put("strip-disk", params.getTmpPath().resolve("strip-disk"));
        taskUtils.readMaskAnnotations(params.getPayload().getImageIds(), maskAnnotationsConfig, imageRepository, fileStorageService, progressInfo, verbose);
    }
}
