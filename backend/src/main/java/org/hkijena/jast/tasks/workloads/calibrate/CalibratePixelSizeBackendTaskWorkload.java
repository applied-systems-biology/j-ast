package org.hkijena.jast.tasks.workloads.calibrate;

import jakarta.transaction.Transactional;
import org.hkijena.jast.config.SystemPackage;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.ViewMode;
import org.hkijena.jast.model.entities.Image;
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
import java.nio.file.Paths;
import java.util.*;

@Component
@BackendTaskType(typeId = "image-calibrate-pixel-size-by-plate")
public class CalibratePixelSizeBackendTaskWorkload implements BackendTaskWorkload {

    private static final List<BackendTaskWorkloadDataSlot> INPUTS = Collections.singletonList(JASTDataSlot.Plate.toSlot());
    private static final List<BackendTaskWorkloadDataSlot> OUTPUTS = Collections.singletonList(JASTDataSlot.PixelSize.toSlot());
    private static final List<BackendTaskWorkloadParameterSlot> PARAMETERS = Collections.singletonList(
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Common, "plate-diameter-mm", "Plate diameter (mm)", "The plate diameter in millimeters", 90));
    private final BackendTaskUtils taskUtils;
    private final ImageRepository imageRepository;
    private final FileStorageService fileStorageService;
    private BackendTaskRegistry registry;

    @Autowired
    public CalibratePixelSizeBackendTaskWorkload(BackendTaskUtils taskUtils, ImageRepository imageRepository, FileStorageService fileStorageService) {
        this.taskUtils = taskUtils;
        this.imageRepository = imageRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void setRegistry(@Lazy BackendTaskRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "Calibrate pixel size by plate";
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
        taskUtils.writeMetadata(params, params.getPayload().getImageIds(), imageRepository, progressInfo, verbose);
        taskUtils.writeMaskAnnotations(params, params.getPayload().getImageIds(), "plate", imageRepository, fileStorageService, progressInfo, verbose);

        Path projectFilePath = taskUtils.writeSharedFile(params, Path.of("workflows","image-calibrate-pixel-size-by-plate.jip"));
        progressInfo.log("Project file is " + projectFilePath);
        taskUtils.runJIPipe(params, projectFilePath, null, "", progressInfo, systemPackages, preferSystemPackages, verbose);

        List<Map<String, String>> updatedMetadata = taskUtils.readCsv(params, Paths.get("metadata_updated.csv"));
        List<Image> toSave = new ArrayList<>();
        for (Map<String, String> map : updatedMetadata) {
            String imageId = map.get("#ImageId");
            String pixelSize = map.get("PixelSize");
            if (imageId != null && pixelSize != null) {
                long imageId_ = Long.parseLong(imageId);
                double pixelSize_ = Double.parseDouble(pixelSize);
                Optional<Image> image = imageRepository.findById(imageId_);
                if (image.isPresent()) {
                    image.get().setPixelSizeMillimeter(pixelSize_);
                    toSave.add(image.get());
                }
            }
        }

        imageRepository.saveAll(toSave);
    }
}
