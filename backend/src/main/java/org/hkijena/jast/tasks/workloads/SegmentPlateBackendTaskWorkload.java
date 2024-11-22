package org.hkijena.jast.tasks.workloads;

import jakarta.transaction.Transactional;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.tasks.*;
import org.hkijena.jast.utils.ProgressInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.*;

@Component
@BackendTaskType(typeId = "image-segment-plate")
public class SegmentPlateBackendTaskWorkload implements BackendTaskWorkload {

    private final ImageRepository imageRepository;
    private static final List<BackendTaskWorkloadDataSlot> INPUTS = Collections.emptyList();
    private static final List<BackendTaskWorkloadDataSlot> OUTPUTS = Collections.singletonList(new BackendTaskWorkloadDataSlot("plate", BackendTaskWorkloadDataSlotType.ImageMaskAnnotation));

    @Autowired
    public SegmentPlateBackendTaskWorkload(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
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
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void execute(BackendTaskWorkloadParams params, ProgressInfo progressInfo) throws Throwable {
        extractRawImages(params, params.getPayload().getImageIds(), imageRepository, progressInfo);
        Path projectFilePath = extractSharedFile(params, "image-segment-plate.jip");
        progressInfo.log("Project file is " + projectFilePath);
        runJIPipe(params, projectFilePath, null, "", progressInfo);

        Map<String, Path> maskAnnotationsConfig = new HashMap<>();
        maskAnnotationsConfig.put("plate", params.getTmpPath().resolve("plate"));
        extractMaskAnnotations(params.getPayload().getImageIds(), maskAnnotationsConfig, imageRepository, progressInfo);
    }
}
