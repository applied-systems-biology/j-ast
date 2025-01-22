package org.hkijena.jast.tasks.workloads;

import jakarta.transaction.Transactional;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.payloads.task.BackendTaskParameterPayload;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.tasks.*;
import org.hkijena.jast.utils.ImageUtils;
import org.hkijena.jast.utils.JASTDataSlot;
import org.hkijena.jast.utils.ProgressInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.*;

@Component
@BackendTaskType(typeId = "invert-raw-image")
public class InvertRawImageBackendTaskWorkload implements BackendTaskWorkload {

    private final ImageRepository imageRepository;
    private static final List<BackendTaskWorkloadDataSlot> INPUTS = List.of(JASTDataSlot.Raw.toSlot());
    private static final List<BackendTaskWorkloadDataSlot> OUTPUTS = List.of();
    private static final List<BackendTaskWorkloadParameterSlot> PARAMETERS = List.of();

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
        List<Image> toSave = new ArrayList<>();
        for (Long imageId : params.getPayload().getImageIds()) {
            progressInfo.log("Processing image " + imageId);
            Optional<Image> image_ = imageRepository.findById(imageId);
            if(image_.isPresent()) {
                Image image = image_.get();
                BufferedImage bufferedImage = ImageUtils.fromPNGBytes(image.getRawData());
                BufferedImage inverted = ImageUtils.invertImage(bufferedImage);
                image.setRawData(ImageUtils.toPNGByteArray(inverted));
                image.setThumbnailData(ImageUtils.toPNGByteArrayThumbnail(inverted));
                image.incrementVersion();
                toSave.add(image);
            }
        }
        imageRepository.saveAll(toSave);
    }
}
