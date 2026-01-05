package org.hkijena.jast.tasks.workloads;

import jakarta.transaction.Transactional;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.ViewMode;
import org.hkijena.jast.payloads.task.BackendTaskPayload;
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
@BackendTaskType(typeId = "image-segment-plate")
public class SegmentPlateBackendTaskWorkload implements BackendTaskWorkload {

    private static final List<BackendTaskWorkloadDataSlot> INPUTS = Collections.emptyList();
    private static final List<BackendTaskWorkloadDataSlot> OUTPUTS = Collections.singletonList(JASTDataSlot.Plate.toSlot());
    private static final List<BackendTaskWorkloadParameterSlot> PARAMETERS = List.of(
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.Boolean, "fastAlgorithm", "Use fast algorithm", "Use a faster algorithm that assumes that there is no visible background behind the plate", false),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.Boolean, "calibrateAfterwards", "Calibrate afterwards", "Automatically calibrate the pixel size to the plate size", true),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.Number, "plate-diameter-mm", "Calibration: Plate diameter (mm)", "The plate diameter in millimeters. Only applies if calibration is enabled.", 90)
    );
    private final ImageRepository imageRepository;
    private final FileStorageService fileStorageService;
    private final BackendTaskUtils taskUtils;
    private BackendTaskRegistry registry;

    @Autowired
    public SegmentPlateBackendTaskWorkload(ImageRepository imageRepository, FileStorageService fileStorageService, BackendTaskUtils taskUtils) {
        this.imageRepository = imageRepository;
        this.fileStorageService = fileStorageService;
        this.taskUtils = taskUtils;
    }

    @Override
    public String getName() {
        return "Auto-detect plate";
    }

    @Override
    public String getDescription() {
        return "Automatically detects the plate for the selected images";
    }

    @Override
    public String getCategory() {
        return "Plate";
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
    public AssayType getAssayTypeRestriction() {
        return AssayType.Unknown;
    }

    @Override
    public ViewMode getViewModeRestriction() {
        return null;
    }

    @Override
    public boolean isOutputsResult() {
        return false;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void execute(BackendTaskWorkloadParams params, ProgressInfo progressInfo) throws Throwable {

        final boolean verbose = params.getRuntimeConfig().isVerbose();

        boolean fastAlgorithm = (boolean) params.getPayload().getParameter("fastAlgorithm").getValue();
        boolean withCalibrate = (boolean) params.getPayload().getParameter("calibrateAfterwards").getValue();
        double plateDiameterMillimeters = ((Number) params.getPayload().getParameter("plate-diameter-mm").getValue()).doubleValue();

        taskUtils.writeRawImages(params, params.getPayload().getImageIds(), imageRepository, fileStorageService, progressInfo, verbose);
        Path projectFilePath = taskUtils.writeSharedFile(params, fastAlgorithm ? "image-segment-plate-fast.jip" : "image-segment-plate.jip");
        progressInfo.log("Project file is " + projectFilePath);
        taskUtils.runJIPipe(params, projectFilePath, null, "", progressInfo, params.getRuntimeConfig().isVerbose());

        Map<String, Path> maskAnnotationsConfig = new HashMap<>();
        maskAnnotationsConfig.put("plate", params.getTmpPath().resolve("plate"));
        taskUtils.readMaskAnnotations(params.getPayload().getImageIds(), maskAnnotationsConfig, imageRepository, fileStorageService, progressInfo, verbose);

        if(withCalibrate) {
            BackendTaskPayload subTaskPayload = new BackendTaskPayload();
            subTaskPayload.setTaskId("image-calibrate-pixel-size-by-plate");
            subTaskPayload.setParameter("plate-diameter-mm", BackendTaskWorkloadParameterSlotType.Number, plateDiameterMillimeters);
            subTaskPayload.setProjectId(params.getPayload().getProjectId());
            subTaskPayload.setImageIds(params.getPayload().getImageIds());
            taskUtils.scheduleSubTask(subTaskPayload, registry, progressInfo);
        }
    }

    @Override
    public void setRegistry(@Lazy BackendTaskRegistry registry) {
        this.registry = registry;
    }
}
