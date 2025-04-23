package org.hkijena.jast.tasks.workloads;

import jakarta.transaction.Transactional;
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
@BackendTaskType(typeId = "image-segment-dda-disk-v3")
public class SegmentDDADiskV3BackendTaskWorkload implements BackendTaskWorkload {

    private static final List<BackendTaskWorkloadDataSlot> INPUTS = Collections.singletonList(JASTDataSlot.Plate.toSlot());
    private static final List<BackendTaskWorkloadDataSlot> OUTPUTS = Collections.singletonList(JASTDataSlot.StripDisk.toSlot());
    private static final List<BackendTaskWorkloadParameterSlot> PARAMETERS = List.of(
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.Number, "expectedDiameter", "Expected diameter (mm)", "Expected disk diameter in millimeters", 6),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.Number, "expectedDiskDiameterMaxDiffPerc", "Allowed diameter difference (%)", "How much the detected objects can deviate from the expected diameter", 0.3),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.Number, "minCirc", "Minimum circularity (%)", "Minimum circularity for object filtering", 0.5)
    );
    private static final Map<String, String> PARAMETER_OVERRIDES = new HashMap<>();

    static {
        PARAMETER_OVERRIDES.put("expectedDiameter", "/expectedDiskDiameter");
        PARAMETER_OVERRIDES.put("expectedDiskDiameterMaxDiffPerc", "/expectedDiskDiameterMaxDiffPerc");
        PARAMETER_OVERRIDES.put("minCirc", "/minCirc");
    }

    private final ImageRepository imageRepository;
    private final FileStorageService fileStorageService;
    private final BackendTaskUtils taskUtils;
    private BackendTaskRegistry registry;

    @Autowired
    public SegmentDDADiskV3BackendTaskWorkload(ImageRepository imageRepository, FileStorageService fileStorageService, BackendTaskUtils taskUtils) {
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
        return "Auto-detect DDA disk (v3)";
    }

    @Override
    public String getDescription() {
        return "Automatically detects the disk in disk diffusion assays. Combination of intensity- and variance-based methods.";
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
        taskUtils.writeRawImages(params, params.getPayload().getImageIds(), imageRepository, fileStorageService, progressInfo);
        taskUtils.writeMaskAnnotations(params, params.getPayload().getImageIds(), "plate", imageRepository, fileStorageService, progressInfo);

        Map<String, Object> parameterOverrides = new HashMap<>();
        for (BackendTaskParameterPayload parameter : params.getPayload().getParameters()) {
            String key = PARAMETER_OVERRIDES.get(parameter.getId());
            if(key != null) {
                parameterOverrides.put(key, parameter.getValue());
            }
        }

        Path projectFilePath = taskUtils.writeSharedFile(params, "image-segment-dda-disk-v3.jip");
        progressInfo.log("Project file is " + projectFilePath);
        taskUtils.runJIPipe(params, projectFilePath, parameterOverrides, "", progressInfo);

        Map<String, Path> maskAnnotationsConfig = new HashMap<>();
        maskAnnotationsConfig.put("strip-disk", params.getTmpPath().resolve("strip-disk"));
        taskUtils.readMaskAnnotations(params.getPayload().getImageIds(), maskAnnotationsConfig, imageRepository, fileStorageService, progressInfo);
    }
}
