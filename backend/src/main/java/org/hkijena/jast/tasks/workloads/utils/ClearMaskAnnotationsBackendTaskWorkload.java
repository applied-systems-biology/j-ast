package org.hkijena.jast.tasks.workloads.utils;

import jakarta.transaction.Transactional;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.ViewMode;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.entities.MaskImageAnnotation;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.repositories.MaskImageAnnotationRepository;
import org.hkijena.jast.services.BackendTaskRegistry;
import org.hkijena.jast.services.BackendTaskUtils;
import org.hkijena.jast.services.FileStorageService;
import org.hkijena.jast.tasks.*;
import org.hkijena.jast.utils.JASTDataSlot;
import org.hkijena.jast.utils.ProgressInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@BackendTaskType(typeId = "clear-mask-annotations")
public class ClearMaskAnnotationsBackendTaskWorkload implements BackendTaskWorkload {

    private static final List<BackendTaskWorkloadDataSlot> INPUTS = List.of(JASTDataSlot.Raw.toSlot());
    private static final List<BackendTaskWorkloadDataSlot> OUTPUTS = List.of(JASTDataSlot.Raw.toSlot());
    private static final List<BackendTaskWorkloadParameterSlot> PARAMETERS = List.of(
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.Boolean, "clear-plate", "Clear plate", "Removes the plate annotations", true),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.Boolean, "clear-strip-disk", "Clear disk/strip", "Removes the DDA disk/E-Test strip", true),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.Boolean, "clear-zoi-shape", "Clear ZOI shape", "Removes the E-Test ZOI shape", true)
    );
    private final ImageRepository imageRepository;
    private final MaskImageAnnotationRepository maskImageAnnotationRepository;
    private final FileStorageService fileStorageService;
    private final BackendTaskUtils taskUtils;
    private BackendTaskRegistry registry;

    @Autowired
    public ClearMaskAnnotationsBackendTaskWorkload(ImageRepository imageRepository, MaskImageAnnotationRepository maskImageAnnotationRepository, FileStorageService fileStorageService, BackendTaskUtils taskUtils) {
        this.imageRepository = imageRepository;
        this.maskImageAnnotationRepository = maskImageAnnotationRepository;
        this.fileStorageService = fileStorageService;
        this.taskUtils = taskUtils;
    }

    @Override
    public void setRegistry(@Lazy BackendTaskRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "Clear mask annotations";
    }

    @Override
    public String getDescription() {
        return "Removes mask annotations from the selected image(s)";
    }

    @Override
    public String getCategory() {
        return "Reset";
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
        boolean clearPlate = params.getPayload().getParameterAsBoolean("clear-plate", true);
        boolean clearStripDisk = params.getPayload().getParameterAsBoolean("clear-strip-disk", true);
        boolean clearZoiShape = params.getPayload().getParameterAsBoolean("clear-zoi-shape", true);

        if(!clearPlate && !clearStripDisk && !clearZoiShape) {
            progressInfo.log("Nothing to do.");
            return;
        }

        List<Long> imageIds = params.getPayload().getImageIds();
        for (int i = 0; i < imageIds.size(); i++) {
            progressInfo.resolveAndLog("Clearing image", i, imageIds.size());
            Long imageId = imageIds.get(i);
            Optional<Image> image_ = imageRepository.findById(imageId);
            if(image_.isPresent()) {
                Image image = image_.get();
                if(clearPlate) {
                    MaskImageAnnotation annotation = image.getMaskImageAnnotation("plate");
                    if(annotation != null) {
                        annotation.resetToEmptyMask(fileStorageService, image);
                        annotation.setVersion(0);
                        maskImageAnnotationRepository.save(annotation);
                    }
                }
                if(clearStripDisk) {
                    MaskImageAnnotation annotation = image.getMaskImageAnnotation("strip-disk");
                    if(annotation != null) {
                        annotation.resetToEmptyMask(fileStorageService, image);
                        annotation.setVersion(0);
                        maskImageAnnotationRepository.save(annotation);
                    }
                }
                if(clearZoiShape) {
                    MaskImageAnnotation annotation = image.getMaskImageAnnotation("zoi-shape");
                    if(annotation != null) {
                        annotation.resetToEmptyMask(fileStorageService, image);
                        annotation.setVersion(0);
                        maskImageAnnotationRepository.save(annotation);
                    }
                }
                image.rebuildThumbnail(fileStorageService);
                image.incrementVersion();
                imageRepository.save(image);
            }
        }

    }
}
