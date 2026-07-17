/*
 * Copyright (c) 2026.
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jast.tasks.workloads;

import com.google.common.collect.ImmutableList;
import jakarta.transaction.Transactional;
import org.hkijena.jast.config.SystemPackage;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.ViewMode;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.payloads.ImagePayload;
import org.hkijena.jast.payloads.ProjectImagesPayload;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.services.BackendTaskRegistry;
import org.hkijena.jast.services.BackendTaskUtils;
import org.hkijena.jast.services.FileStorageService;
import org.hkijena.jast.tasks.*;
import org.hkijena.jast.utils.JASTDataSlot;
import org.hkijena.jast.utils.JsonUtils;
import org.hkijena.jast.utils.ProgressInfo;
import org.hkijena.jast.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Component
@BackendTaskType(typeId = "aio-prepare")
public class AllInOnePreparationBackendTaskWorkload implements BackendTaskWorkload {

    private static final List<BackendTaskWorkloadDataSlot> INPUTS = Collections.emptyList();
    private static final List<BackendTaskWorkloadDataSlot> OUTPUTS = List.of(
            JASTDataSlot.Plate.toSlot(),
            JASTDataSlot.StripDisk.toSlot(),
            JASTDataSlot.ZOIShape.toSlot(),
            JASTDataSlot.PixelSize.toSlot());
    private static final List<BackendTaskWorkloadParameterSlot> PARAMETERS = List.of(
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Boolean, BackendTaskWorkloadParameterSlotType.Common, "do-autofill", "Enable auto-fill metadata (if needed)", "If enabled, the operation will try to extract the assay type, experiment, and sample from the image names. If an image already has metadata, nothing will be changed.", true),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Boolean, BackendTaskWorkloadParameterSlotType.Common, "do-autosort", "Enable auto-sort by metadata (if needed)", "If enabled, the operation will attempt to sort metadata into time lines. If an image is already sorted, nothing will be changed.", true),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Boolean, BackendTaskWorkloadParameterSlotType.Common, "do-find-plate", "Enable plate finder (+ calibrate)", "If enabled, find the plate with an automated operation. Will also calibrate the pixel size.", true),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Boolean, BackendTaskWorkloadParameterSlotType.Common, "do-find-dda-disk", "Enable DDA disk finder", "If enabled, find the disks inside DDA images", true),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Boolean, BackendTaskWorkloadParameterSlotType.Common, "do-find-etest-strips", "Enable E-Test strip finder", "If enabled, find the strips inside E-test images. Please note that this algorithm might not always work perfectly.", true),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Boolean, BackendTaskWorkloadParameterSlotType.Common, "do-find-etest-zoi-shape", "Enable E-Test ZOI shape finder", "If enabled, find the E-test ZOI shapes. Please note that this algorithm is very experimental and doesn't always work.", true),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.String, BackendTaskWorkloadParameterSlotType.Common, "time-points", "Time points", "List of time points in order. Separate with a space. Can include additional values (will be ignored).", "24hr 24 48hr 48"),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.String, BackendTaskWorkloadParameterSlotType.Common, "metadata-key-dda", "Keywords (DDA)", "List of possible key words that identify a DDA.  Separate with space.", "DDA"),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.String, BackendTaskWorkloadParameterSlotType.Common, "metadata-key-etest", "Keywords (E-Test)", "List of possible key words that identify an E-Test. Separate with a space", "E-Test ETest"),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.String, BackendTaskWorkloadParameterSlotType.Common, "metadata-default-assay-type", "Default assay type (DDA/ETest)", "Set to DDA or ETest", "DDA"),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.String, BackendTaskWorkloadParameterSlotType.Common, "filename-delimiters", "File name delimiters", "For automated metadata filling.", "-_;."),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Common, "plate-diameter-mm", "Plate diameter (mm)", "The plate diameter in millimeters. Used for calibrating the pixel size.", 90),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Common, "dda-disk-diameter-mm", "DDA disk diameter (mm)", "The diameter of the DDA disk.", 6));
    private final BackendTaskUtils taskUtils;
    private final ImageRepository imageRepository;
    private final ProjectRepository projectRepository;
    private final FileStorageService fileStorageService;
    private BackendTaskRegistry registry;

    @Autowired
    public AllInOnePreparationBackendTaskWorkload(BackendTaskUtils taskUtils, ImageRepository imageRepository, ProjectRepository projectRepository, FileStorageService fileStorageService) {
        this.taskUtils = taskUtils;
        this.imageRepository = imageRepository;
        this.projectRepository = projectRepository;
        this.fileStorageService = fileStorageService;
    }

    private static void autoSortPayload(ProgressInfo progressInfo, ProjectImagesPayload payload, List<String> timePointOrder) {
        // Identify which images we can actually auto-sort
        List<ImagePayload> toSort = payload.getImagesById().values().stream().filter(imagePayload -> {
            if (StringUtils.isNullOrEmpty(imagePayload.getTimePoint()) || !timePointOrder.contains(imagePayload.getTimePoint())) {
                return false;
            }
            if (StringUtils.isNullOrEmpty(imagePayload.getExperiment())) {
                return false;
            }
            if (StringUtils.isNullOrEmpty(imagePayload.getSample())) {
                return false;
            }
            if (imagePayload.getAssayType() == AssayType.Unknown) {
                return false;
            }
            return true;
        }).toList();
        progressInfo.log("Auto-sort will attempt to sort " + toSort.size() + " images");
        Set<String> allTimePointsToSort = toSort.stream().map(ImagePayload::getTimePoint).collect(Collectors.toSet());
        List<String> finalTimePoints = timePointOrder.stream().filter(allTimePointsToSort::contains).toList();
        if (!toSort.isEmpty() && !finalTimePoints.isEmpty()) {
            progressInfo.log("Sorting with time points: " + JsonUtils.toJsonString(finalTimePoints));
            payload.autoSort(toSort, finalTimePoints, progressInfo.resolve("Auto sort"));
        } else {
            progressInfo.log("ERROR: Nothing to sort. Either no images or no time points!");
        }
    }

    private static void autofillPayload(BackendTaskWorkloadParams params, ProgressInfo progressInfo, ProjectImagesPayload payload, AssayType defaultAssayType, List<String> timePointOrder) {
        final String delimiters = params.getPayload().getParameterAsString("filename-delimiters", "-_;.");
        final Set<String> ddaKeywords = Set.of(params.getPayload().getParameterAsString("metadata-key-dda", "DDA dda").split(" "));
        final Set<String> etestKeywords = Set.of(params.getPayload().getParameterAsString("metadata-key-etest", "ETest E-test").split(" "));

        for (ImagePayload value : payload.getImagesById().values()) {

            ProgressInfo imageProgress = progressInfo.resolve(value.getFileName());

            String str = value.getFileName();
            str = str.replace(".png", "");
            str = str.replace(".jpg", "");
            str = str.replace(".jpeg", "");

            // Try to find the assay type
            if (value.getAssayType() == AssayType.Unknown) {
                for (String ddaKeyword : ddaKeywords) {
                    if (str.toLowerCase().contains(ddaKeyword.toLowerCase())) {
                        value.setAssayType(AssayType.DDA);
                        break;
                    }
                }
                if (value.getAssayType() == AssayType.Unknown) {
                    for (String etestKeyword : etestKeywords) {
                        if (str.toLowerCase().contains(etestKeyword.toLowerCase())) {
                            value.setAssayType(AssayType.ETest);
                        }
                    }
                    if (value.getAssayType() == AssayType.Unknown) {
                        value.setAssayType(defaultAssayType);
                    }
                }
            }
            imageProgress.log("Mapped to assay type " + value.getAssayType());

            // Erase the assay type keywords from the string
            for (String ddaKeyword : ddaKeywords) {
                str = StringUtils.replaceAllIgnoreCase(str, ddaKeyword, "");
            }
            for (String etestKeyword : etestKeywords) {
                str = StringUtils.replaceAllIgnoreCase(str, etestKeyword, "");
            }

            List<String> splitItems = new ArrayList<>(List.of(str.split("[" + delimiters + "]+")));

            // Try to find the time point
            if (StringUtils.isNullOrEmpty(value.getTimePoint())) {
                for (int i = splitItems.size() - 1; i >= 0; i--) {
                    String splitItem = splitItems.get(i);
                    if (timePointOrder.contains(splitItem)) {
                        value.setTimePoint(splitItem);
                        break;
                    }
                }
                imageProgress.log("Assigned TimePoint=" + value.getTimePoint());
            }

            // Erase time points
            splitItems.removeIf(timePointOrder::contains);

            // Experiment and sample
            if (StringUtils.isNullOrEmpty(value.getExperiment()) && StringUtils.isNullOrEmpty(value.getSample())) {
                splitItems.removeIf(item -> item.trim().isEmpty());
                imageProgress.log("Split file name into " + JsonUtils.toJsonString(splitItems));

                if (splitItems.size() > 1) {
                    value.setExperiment(splitItems.get(0));
                    value.setSample(splitItems.stream().skip(1).collect(Collectors.joining("_")));
                    imageProgress.log("Assigned Experiment=" + value.getExperiment() + ", Sample=" + value.getSample());
                } else if (splitItems.size() == 1) {
                    value.setExperiment(splitItems.get(0));
                    imageProgress.log("WARNING: Only one metadata item! Using this as experiment!");
                } else {
                    imageProgress.log("ERROR: Unable to find metadata mapping (not enough metadata!)");
                }
            }
        }
    }

    @Override
    public void setRegistry(@Lazy BackendTaskRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "All-in-one preparation";
    }

    @Override
    public String getDescription() {
        return "Automatically fills in the metadata (if needed), sorts the images (if needed), finds the plate, the DDA disk/E-test strip, and E-Test ZOI shape";
    }

    @Override
    public String getCategory() {
        return "";
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
        final boolean preferSystemPackages = params.getRuntimeConfig().isPreferSystemPackages();
        final List<SystemPackage> systemPackages = params.getRuntimeConfig().getSystemPackages();

        final boolean doAutofill = params.getPayload().getParameterAsBoolean("do-autofill", true);
        final boolean doAutosort = params.getPayload().getParameterAsBoolean("do-autosort", true);
        final boolean doFindCalibratePlate = params.getPayload().getParameterAsBoolean("do-find-plate", true);
        final boolean doFindDDADisk = params.getPayload().getParameterAsBoolean("do-find-dda-disk", true);
        final boolean doFindETestStrip = params.getPayload().getParameterAsBoolean("do-find-etest-strips", true);
        final boolean doFindETestZOIShape = params.getPayload().getParameterAsBoolean("do-find-etest-zoi-shape", true);
        final List<String> timePointOrder = Arrays.asList(params.getPayload().getParameterAsString("time-points", "24hr 24 48hr 48").split("[\\s,;]"));
        final AssayType defaultAssayType = AssayType.valueOf(params.getPayload().getParameterAsString("default-assay-type", "DDA"));

        progressInfo.log("Detected time point order: " + JsonUtils.toJsonString(timePointOrder));

        Project project = projectRepository.findById(params.getPayload().getProjectId()).get();

        // Create a payload
        ProjectImagesPayload payload = new ProjectImagesPayload(project);

        // Do auto-fill-in metadata
        if (doAutofill) {
            autofillPayload(params, progressInfo, payload, defaultAssayType, timePointOrder);
        }

        // Do auto-sort
        if (doAutosort) {
            autoSortPayload(progressInfo, payload, timePointOrder);
        }

        Map<Long, Image> imageMap = new HashMap<>();
        for (Image image : ImmutableList.copyOf(imageRepository.findAllById(params.getPayload().getImageIds()))) {
            imageMap.put(image.getId(), image);
        }

        // Write payload back into the database and update the local entities
        if (doAutofill || doAutosort) {
            for (Map.Entry<Long, ImagePayload> entry : payload.getImagesById().entrySet()) {
                Image imageEntity = imageMap.get(entry.getKey());
                ImagePayload imagePayload = entry.getValue();
                imageEntity.updateFromPayload(imagePayload);
            }
            for (Image image : ImmutableList.copyOf(imageRepository.saveAll(imageMap.values()))) {
                imageMap.put(image.getId(), image);
            }
        }

        if (doFindCalibratePlate) {
            List<Long> idsToProcess = filterValidImageIds(params.getPayload().getImageIds(), imageMap);
            if (!idsToProcess.isEmpty()) {
                // Find plate
                ProgressInfo plateProgress = progressInfo.resolve("Find/calibrate plate");
                {
                    taskUtils.clearTmp(params.getTmpPath(), plateProgress);
                    taskUtils.writeRawImages(params, idsToProcess, imageRepository, fileStorageService, plateProgress, verbose);
                    taskUtils.writeMetadata(params, idsToProcess, imageRepository, plateProgress, verbose);
                    Path projectFilePath = taskUtils.writeSharedFile(params, Path.of("workflows", "image-segment-plate-fast.jip"));
                    plateProgress.log("Project file is " + projectFilePath);
                    taskUtils.runJIPipe(params, projectFilePath, null, "", plateProgress, systemPackages, preferSystemPackages, verbose);

                    Map<String, Path> maskAnnotationsConfig = new HashMap<>();
                    maskAnnotationsConfig.put("plate", params.getTmpPath().resolve("plate"));
                    taskUtils.readMaskAnnotations(idsToProcess, maskAnnotationsConfig, imageRepository, fileStorageService, plateProgress, verbose);
                }
                // Calibrate
                {
                    taskUtils.clearTmp(params.getTmpPath(), plateProgress);
                    taskUtils.writeRawImages(params, idsToProcess, imageRepository, fileStorageService, plateProgress, verbose);
                    taskUtils.writeMetadata(params, idsToProcess, imageRepository, plateProgress, verbose);
                    taskUtils.writeMaskAnnotations(params, idsToProcess, "plate", imageRepository, fileStorageService, plateProgress, verbose);

                    Path projectFilePath = taskUtils.writeSharedFile(params, Path.of("workflows", "image-calibrate-pixel-size-by-plate.jip"));
                    plateProgress.log("Project file is " + projectFilePath);
                    taskUtils.runJIPipe(params, projectFilePath, null, "", plateProgress, systemPackages, preferSystemPackages, verbose);

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
            } else {
                progressInfo.log("Find/calibrate plate: nothing to do!");
            }
        }

        if (doFindDDADisk) {
            List<Long> idsToProcess = filterDDAImageIds(params.getPayload().getImageIds(), imageMap);
            if (!idsToProcess.isEmpty()) {
                ProgressInfo ddaProgress = progressInfo.resolve("Find DDA disk");
                taskUtils.clearTmp(params.getTmpPath(), ddaProgress);

                taskUtils.writeRawImages(params, idsToProcess, imageRepository, fileStorageService, ddaProgress, verbose);
                taskUtils.writeMetadata(params, idsToProcess, imageRepository, ddaProgress, verbose);
                taskUtils.writeMaskAnnotations(params, idsToProcess, "plate", imageRepository, fileStorageService, ddaProgress, verbose);

                Path projectFilePath = taskUtils.writeSharedFile(params, Path.of("workflows", "image-segment-dda-disk-v3.jip"));
                ddaProgress.log("Project file is " + projectFilePath);
                Map<String, Object> ddaParameters = new HashMap<>();
                ddaParameters.put("/expectedDiskDiameter", params.getPayload().getParameterAsDouble("dda-disk-diameter-mm", 6));
                taskUtils.runJIPipe(params, projectFilePath, ddaParameters, "", ddaProgress, systemPackages, preferSystemPackages, verbose);

                Map<String, Path> maskAnnotationsConfig = new HashMap<>();
                maskAnnotationsConfig.put("strip-disk", params.getTmpPath().resolve("strip-disk"));
                taskUtils.readMaskAnnotations(idsToProcess, maskAnnotationsConfig, imageRepository, fileStorageService, ddaProgress, verbose);
            } else {
                progressInfo.log("Find DDA disk: nothing to do!");
            }
        }

        if (doFindETestStrip) {
            List<Long> idsToProcess = filterETestImageIds(params.getPayload().getImageIds(), imageMap);
            if (!idsToProcess.isEmpty()) {
                ProgressInfo etestProgress = progressInfo.resolve("Find E-Test strip");
                taskUtils.clearTmp(params.getTmpPath(), etestProgress);

                taskUtils.writeRawImages(params, idsToProcess, imageRepository, fileStorageService, etestProgress, verbose);
                taskUtils.writeMetadata(params, idsToProcess, imageRepository, etestProgress, verbose);
                taskUtils.writeMaskAnnotations(params, idsToProcess, "plate", imageRepository, fileStorageService, etestProgress, verbose);

                Path projectFilePath = taskUtils.writeSharedFile(params, Path.of("workflows", "image-segment-etest-strip.jip"));
                etestProgress.log("Project file is " + projectFilePath);
                taskUtils.runJIPipe(params, projectFilePath, Collections.emptyMap(), "", etestProgress, systemPackages, preferSystemPackages, verbose);

                Map<String, Path> maskAnnotationsConfig = new HashMap<>();
                maskAnnotationsConfig.put("strip-disk", params.getTmpPath().resolve("strip-disk"));
                taskUtils.readMaskAnnotations(idsToProcess, maskAnnotationsConfig, imageRepository, fileStorageService, etestProgress, verbose);
            } else {
                progressInfo.log("Find E-test strip: nothing to do!");
            }
        }

        if (doFindETestZOIShape) {
            List<Long> idsToProcess = filterFirstETestImageIds(params.getPayload().getImageIds(), imageMap);
            if (!idsToProcess.isEmpty()) {
                ProgressInfo zoiShapeProgress = progressInfo.resolve("Find ZOI shape");
                taskUtils.clearTmp(params.getTmpPath(), zoiShapeProgress);
                taskUtils.writeRawImages(params, idsToProcess, imageRepository, fileStorageService, zoiShapeProgress, verbose);
                taskUtils.writeMetadata(params, idsToProcess, imageRepository, zoiShapeProgress, verbose);
                taskUtils.writeMaskAnnotations(params, idsToProcess, "plate", imageRepository, fileStorageService, zoiShapeProgress, verbose);
                taskUtils.writeMaskAnnotations(params, idsToProcess, "strip-disk", imageRepository, fileStorageService, zoiShapeProgress, verbose);

                Path projectFilePath = taskUtils.writeSharedFile(params, Path.of("workflows", "image-segment-zoi-shape.jip"));
                zoiShapeProgress.log("Project file is " + projectFilePath);
                taskUtils.runJIPipe(params, projectFilePath, Collections.emptyMap(), "", zoiShapeProgress, systemPackages, preferSystemPackages, verbose);

                Map<String, Path> maskAnnotationsConfig = new HashMap<>();
                maskAnnotationsConfig.put("zoi-shape", params.getTmpPath().resolve("zoi-shape"));
                taskUtils.readMaskAnnotations(idsToProcess, maskAnnotationsConfig, imageRepository, fileStorageService, zoiShapeProgress, verbose);
            } else {
                progressInfo.log("Find E-test ZOI shape: nothing to do!");
            }
        }
        if (doFindETestZOIShape) {
            List<Long> idsToProcess = filterETestImageIds(params.getPayload().getImageIds(), imageMap);
            if (!idsToProcess.isEmpty()) {
                ProgressInfo zoiShapeProgress = progressInfo.resolve("Register ZOI shape");
                taskUtils.clearTmp(params.getTmpPath(), zoiShapeProgress);

                taskUtils.writeRawImages(params, idsToProcess, imageRepository, fileStorageService, zoiShapeProgress, verbose);
                taskUtils.writeMetadata(params, idsToProcess, imageRepository, zoiShapeProgress, verbose);
                taskUtils.writeMaskAnnotations(params, idsToProcess, "strip-disk", imageRepository, fileStorageService, zoiShapeProgress, verbose);
                taskUtils.writeRowFirstMaskAnnotations(params, idsToProcess, "zoi-shape", imageRepository, fileStorageService, zoiShapeProgress, verbose);

                Path projectFilePath = taskUtils.writeSharedFile(params, Path.of("workflows", "etest-copy-registered-zoi-shape.jip"));
                zoiShapeProgress.log("Project file is " + projectFilePath);
                taskUtils.runJIPipe(params, projectFilePath, Collections.emptyMap(), "", zoiShapeProgress, systemPackages, preferSystemPackages, verbose);

                Map<String, Path> maskAnnotationsConfig = new HashMap<>();
                maskAnnotationsConfig.put("zoi-shape", params.getTmpPath().resolve("zoi-shape-aligned"));
                taskUtils.readMaskAnnotations(idsToProcess, maskAnnotationsConfig, imageRepository, fileStorageService, zoiShapeProgress, verbose);
            } else {
                progressInfo.log("Register E-test ZOI shape: nothing to do!");
            }
        }


    }

    private List<Long> filterValidImageIds(List<Long> imageIds, Map<Long, Image> imageMap) {
        return imageIds.stream().filter(id -> imageMap.get(id).getAssayType() != AssayType.Unknown && imageMap.get(id).getGroupRow() >= 0).toList();
    }

    private List<Long> filterDDAImageIds(List<Long> imageIds, Map<Long, Image> imageMap) {
        return imageIds.stream().filter(id -> imageMap.get(id).getAssayType() == AssayType.DDA && imageMap.get(id).getGroupRow() >= 0).toList();
    }

    private List<Long> filterETestImageIds(List<Long> imageIds, Map<Long, Image> imageMap) {
        return imageIds.stream().filter(id -> imageMap.get(id).getAssayType() == AssayType.ETest && imageMap.get(id).getGroupRow() >= 0).toList();
    }

    private List<Long> filterFirstETestImageIds(List<Long> imageIds, Map<Long, Image> imageMap) {
        return imageIds.stream().filter(id -> imageMap.get(id).getAssayType() == AssayType.ETest && imageMap.get(id).getGroupRow() >= 0 && imageMap.get(id).getGroupColumn() == 0).toList();
    }
}
