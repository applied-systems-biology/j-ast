package org.hkijena.jast.tasks.workloads;

import jakarta.transaction.Transactional;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.payloads.task.BackendTaskParameterPayload;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.services.FileStorageService;
import org.hkijena.jast.tasks.*;
import org.hkijena.jast.utils.JASTDataSlot;
import org.hkijena.jast.utils.ProgressInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@BackendTaskType(typeId = "image-segment-dda-disk")
public class SegmentDDADiskBackendTaskWorkload implements BackendTaskWorkload {

    private static final List<BackendTaskWorkloadDataSlot> INPUTS = Collections.singletonList(JASTDataSlot.Plate.toSlot());
    private static final List<BackendTaskWorkloadDataSlot> OUTPUTS = Collections.singletonList(JASTDataSlot.StripDisk.toSlot());
    private static final List<BackendTaskWorkloadParameterSlot> PARAMETERS = List.of(
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.Number, "minCirc", "Minimum circularity (0-1)", "Minimum circularity of the disk", 0.5),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.Number, "minFeret", "Minimum diameter (mm)", "Minimum diameter in millimeters. A lower than expected value is better.", 2),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.Number, "maxFeret", "Maximum diameter (mm)", "Maximum diameter in millimeters. A higher than expected value is better.", 12)
    );
    private static final Map<String, String> PARAMETER_OVERRIDES = new HashMap<>();

    static {
        PARAMETER_OVERRIDES.put("minCirc", "dc2e186c-881c-42d1-a9b6-78a8f24579ec/exported/circle filter/minCirc");
        PARAMETER_OVERRIDES.put("minFeret", "dc2e186c-881c-42d1-a9b6-78a8f24579ec/exported/circle filter/minFeret");
        PARAMETER_OVERRIDES.put("maxFeret", "dc2e186c-881c-42d1-a9b6-78a8f24579ec/exported/circle filter/maxFeret");
    }

    private final ImageRepository imageRepository;
    private final FileStorageService fileStorageService;

    @Autowired
    public SegmentDDADiskBackendTaskWorkload(ImageRepository imageRepository, FileStorageService fileStorageService) {
        this.imageRepository = imageRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public String getName() {
        return "Auto-detect DDA disk";
    }

    @Override
    public String getDescription() {
        return "Automatically detects the disk in disk diffusion assays";
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
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void execute(BackendTaskWorkloadParams params, ProgressInfo progressInfo) throws Throwable {
        writeRawImages(params, params.getPayload().getImageIds(), imageRepository, fileStorageService, progressInfo);
        writeMaskAnnotations(params, params.getPayload().getImageIds(), "plate", imageRepository, fileStorageService, progressInfo);

        Map<String, Object> parameterOverrides = new HashMap<>();
        for (BackendTaskParameterPayload parameter : params.getPayload().getParameters()) {
            parameterOverrides.put(PARAMETER_OVERRIDES.get(parameter.getId()), parameter.getValue());
        }

        Path projectFilePath = writeSharedFile(params, "image-segment-dda-disk.jip");
        progressInfo.log("Project file is " + projectFilePath);
        runJIPipe(params, projectFilePath, parameterOverrides, "", progressInfo);

        Map<String, Path> maskAnnotationsConfig = new HashMap<>();
        maskAnnotationsConfig.put("strip-disk", params.getTmpPath().resolve("strip-disk"));
        readMaskAnnotations(params.getPayload().getImageIds(), maskAnnotationsConfig, imageRepository, fileStorageService, progressInfo);
    }
}
