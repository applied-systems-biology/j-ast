package org.hkijena.jast.tasks.workloads;

import jakarta.transaction.Transactional;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.tasks.*;
import org.hkijena.jast.utils.ImageUtils;
import org.hkijena.jast.utils.JASTDataSlot;
import org.hkijena.jast.utils.ProgressInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@BackendTaskType(typeId = "invert-raw-image")
public class InvertRawImageBackendTaskWorkload implements BackendTaskWorkload {

    private static final List<BackendTaskWorkloadDataSlot> INPUTS = List.of(JASTDataSlot.Raw.toSlot());
    private static final List<BackendTaskWorkloadDataSlot> OUTPUTS = List.of(JASTDataSlot.Raw.toSlot());
    private static final List<BackendTaskWorkloadParameterSlot> PARAMETERS = List.of();
    private final ImageRepository imageRepository;

    @Autowired
    public InvertRawImageBackendTaskWorkload(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    @Override
    public String getName() {
        return "Invert raw image";
    }

    @Override
    public String getDescription() {
        return "Inverts the pixel values in the raw image";
    }

    @Override
    public String getCategory() {
        return "Preprocessing";
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
    public boolean isOutputsResult() {
        return false;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void execute(BackendTaskWorkloadParams params, ProgressInfo progressInfo) throws Throwable {
        writeRawImages(params, params.getPayload().getImageIds(), imageRepository, progressInfo);

        Path projectFilePath = writeSharedFile(params, "invert-image.jip");
        progressInfo.log("Project file is " + projectFilePath);
        runJIPipe(params, projectFilePath, Collections.emptyMap(), "", progressInfo);

        readRawImages(params.getPayload().getImageIds(), params.getTmpPath().resolve("raw_updated"), imageRepository, progressInfo);
    }
}
