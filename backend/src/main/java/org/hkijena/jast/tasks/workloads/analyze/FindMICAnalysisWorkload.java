package org.hkijena.jast.tasks.workloads.analyze;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Predicates;
import jakarta.transaction.Transactional;
import org.hkijena.jast.config.SystemPackage;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.ViewMode;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.repositories.ResultRepository;
import org.hkijena.jast.services.BackendTaskRegistry;
import org.hkijena.jast.services.BackendTaskUtils;
import org.hkijena.jast.services.FileStorageService;
import org.hkijena.jast.services.ImageMetadata;
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
@BackendTaskType(typeId = "find-mic")
public class FindMICAnalysisWorkload implements BackendTaskWorkload {

    private static final List<BackendTaskWorkloadDataSlot> INPUTS = List.of(
            JASTDataSlot.Plate.toSlot(),
            JASTDataSlot.StripDisk.toSlot(),
            JASTDataSlot.StripPreset.toSlot());
    private static final List<BackendTaskWorkloadDataSlot> OUTPUTS = Collections.emptyList();
    private static final List<BackendTaskWorkloadParameterSlot> PARAMETERS = List.of(
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.String, BackendTaskWorkloadParameterSlotType.Common, "result-name", "Result name", "The name of the generated result folder", "MIC analysis"),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.String, BackendTaskWorkloadParameterSlotType.Common, "result-description", "Result description", "Description of the generated result", ""),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Advanced, "bgr-subtraction-start-radius", "OCR Background subtraction start radius (px)", "The lowest background subtraction radius", 20),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Advanced, "bgr-subtraction-end-radius", "OCR Background subtraction end radius (px)", "The highest background subtraction radius", 64),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Advanced, "bgr-subtraction-radius-increment", "OCR Background subtraction radius increment (px)", "Increment for the background subtraction radius sweep", 4),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Advanced, "confidence-threshold", "OCR Confidence threshold (%)", "The minimum confidence for the OCR algorithm results", 90),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Advanced, "target-dpi", "Target DPI", "The target DPI for image upscaling. OCR algorithms require a high DPI to work properly.", 600),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Common, "strip-safety-distance", "Exclusion distance around strip (mm)", "The area around the strip annotation that is excluded from the measurement process. Must be large enough to exclude the strip itself.", 1),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Common, "strip-measure-distance", "Measurement distance around strip (mm)", "The area around the strip annotation that is used for the measurement process.", 10),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Boolean, BackendTaskWorkloadParameterSlotType.Advanced, "add-inverted-candidates", "OCR Generate inverted OCR inputs (slow)", "If enabled, create also inverted images for OCR processing. Doubles the required processing time", false),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Boolean, BackendTaskWorkloadParameterSlotType.Common, "write-mic", "Set MIC metadata", "If enabled, set the MIC metadata of the image from the calculated values.", true),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Common, "corridor-width-narrow", "Corridor width narrow (mm)", "The measurement corridor width for narrow-ZOI images", 0.3),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Common, "corridor-width-wide", "Corridor width wide (mm)", "The measurement corridor width for wide-ZOI images", 3),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Advanced, "inverse-detection-min-concentrations", "Inverse detection min reference value number", "Number of distinct concentration points required for the detector to work", 3),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Advanced, "inverse-detection-var-threshold", "Inverse detection variance threshold", "The minimum variation required for the detector to work", 0.05),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Common, "mic-threshold", "MIC threshold", "The threshold to applied to the normalized growth that indicates the minimum inhibitory concentration", 0.255),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Advanced, "mic-threshold-n-references", "MIC threshold reference concentration number", "The number of highest and lowest concentration values chosen as reference for growth normalization", 3),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Advanced, "na-detection-close-boundary-threshold", "N/A detection close ZOI boundary threshold (mm)", "If the ZOI is detected to be closer to the strip than this value, the image is flagged as N/A", 1.5),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Advanced, "na-detection-contrast-threshold", "N/A detection contrast threshold", "If the growth range (Michelson contrast) is below the given value in either the wide or narrow corridor the image is detected as N/A", 0.44),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Advanced, "na-detection-max-cov-at-high", "N/A detection max CV at high concentrations", "If the coefficient of variation at high concentrations is above the specified limit, this indicates growth inside the ZOI, marking the image as N/A", 0.15),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Advanced, "ring-width", "Ring width (mm)", "The horizontal spatial resolution of measurements, forming multiple 'rings' around the strip", 0.3),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Advanced, "ring-width-max", "Ring extent (mm)", "How much the measurement area extends from the strip edge. Should be larger than the ZOI edge distance.", 30),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Advanced, "scale-factor", "Scale factor", "Scale the image prior to MIC measurement (after DPI-aware scaling). Lower values can enhance the performance at the cost of precision", 1),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Advanced, "zoi-boundary-lawn-fraction", "ZOI boundary lawn fraction", "How much the intensity gain from the ZOI ins required for the finding the boundary", 0.3),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Advanced, "zoi-boundary-min-inhibition-threshold", "ZOI boundary min inhibition threshold (mm)", "Excludes the specified area around the strop from the ZOI boundary detection", 0.5),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Advanced, "zoi-boundary-wide-threshold", "ZOI boundary wide classification threshold (mm)", "If the maximum ZOI boundary is wider than the specified value, the image is classified as having a wide ZOI", 7.5),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Boolean, BackendTaskWorkloadParameterSlotType.Common, "enable-na-detection", "Enable N/A detection", "If enabled, the algorithm will attempt to detect whether an image has no MIC (N/A) based on contrast, intensity variation and ZOI boundary", true)
    );

    private final ImageRepository imageRepository;
    private final ProjectRepository projectRepository;
    private final ResultRepository resultRepository;
    private final FileStorageService fileStorageService;
    private final BackendTaskUtils taskUtils;
    private BackendTaskRegistry registry;

    @Autowired
    public FindMICAnalysisWorkload(ImageRepository imageRepository, ProjectRepository projectRepository, ResultRepository resultRepository, FileStorageService fileStorageService, BackendTaskUtils taskUtils) {
        this.imageRepository = imageRepository;
        this.projectRepository = projectRepository;
        this.resultRepository = resultRepository;
        this.fileStorageService = fileStorageService;
        this.taskUtils = taskUtils;
    }

    @Override
    public ViewMode getViewModeRestriction() {
        return null;
    }

    @Override
    public void setRegistry(@Lazy BackendTaskRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "Auto-detect MIC v1";
    }

    @Override
    public String getShortDescription() {
        return "Attempts to find the minimum inhibitory concentration (MIC)";
    }

    @Override
    public String getDescription() {
        return "Applies an optical character recognition (OCR) approach to align the ticks on the strip to the provided reference sequence. " +
                "The area around the strip given by the minimum and maximum distance is used to capture the average colony intensity for a given concentration. " +
                "Based on the resulting measurements the MIC is determined and stored within the generated results and the image." +
                "Please note that the pixel size must be correctly calibrated for the analysis to work. " +
                "Please note that each E-Test needs to be annotated with a strip preset that contains the number sequence and other metadata.";
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
        return "Analyze";
    }

    @Override
    public AssayType getAssayTypeRestriction() {
        return AssayType.ETest;
    }

    @Override
    public boolean isOutputsResult() {
        return true;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void execute(BackendTaskWorkloadParams params, ProgressInfo progressInfo) throws Throwable {
        final boolean verbose = params.getRuntimeConfig().isVerbose();
        final boolean preferSystemPackages = params.getRuntimeConfig().isPreferSystemPackages();
        final List<SystemPackage> systemPackages = params.getRuntimeConfig().getSystemPackages();

        Map<String, Object> parameterOverrides = new HashMap<>();

        taskUtils.writeRawImages(params, params.getPayload().getImageIds(), imageRepository, fileStorageService, progressInfo, verbose);
        taskUtils.writeMetadata(params, params.getPayload().getImageIds(), imageRepository, progressInfo, verbose);
        taskUtils.writeMaskAnnotations(params, params.getPayload().getImageIds(), "plate", imageRepository, fileStorageService, progressInfo, verbose);
        taskUtils.writeMaskAnnotations(params, params.getPayload().getImageIds(), "strip-disk", imageRepository, fileStorageService, progressInfo, verbose);

        final String nodeId = "40571954-21a9-4996-96d7-a4c84217063d";
        boolean withInvertedImages = params.getPayload().getParameterAsBoolean("add-inverted-candidates", false);

        parameterOverrides.put("/bgrSubtractionStartRadius", params.getPayload().getParameterAsDouble("bgr-subtraction-start-radius", 20));
        parameterOverrides.put("/bgrSubtractionEndRadius", params.getPayload().getParameterAsDouble("bgr-subtraction-end-radius", 64));
        parameterOverrides.put("/bgrSubtractionRadiusIncrement", params.getPayload().getParameterAsDouble("bgr-subtraction-radius-increment", 4));
        parameterOverrides.put("/confidenceThreshold", params.getPayload().getParameterAsDouble("confidence-threshold", 90));
        parameterOverrides.put("/targetDPI", params.getPayload().getParameterAsDouble("target-dpi", 600));
        parameterOverrides.put("/stripSafetyDistanceMillimeters", params.getPayload().getParameterAsDouble("strip-safety-distance", 1));
        parameterOverrides.put("/stripMeasurementDistanceMillimeters", params.getPayload().getParameterAsDouble("strip-measure-distance", 10));
        parameterOverrides.put("/handleInvertedImages", withInvertedImages);

        // Node parameters for MIC analysis
        parameterOverrides.put(nodeId + "/script-parameters/corridorWidthNarrowMillimeters", params.getPayload().getParameterAsDouble("corridor-width-narrow", 0.3));
        parameterOverrides.put(nodeId + "/script-parameters/corridorWidthWideMillimeters", params.getPayload().getParameterAsDouble("corridor-width-wide", 3));
        parameterOverrides.put(nodeId + "/script-parameters/inverseDetectionMinConcentrations", params.getPayload().getParameterAsDouble("inverse-detection-min-concentrations", 3));
        parameterOverrides.put(nodeId + "/script-parameters/inverseDetectionVariationThreshold", params.getPayload().getParameterAsDouble("inverse-detection-var-threshold", 0.05));
        parameterOverrides.put(nodeId + "/script-parameters/micThreshold", params.getPayload().getParameterAsDouble("mic-threshold", 0.255));
        parameterOverrides.put(nodeId + "/script-parameters/micThresholdNReference", params.getPayload().getParameterAsDouble("mic-threshold-n-references", 3));
        parameterOverrides.put(nodeId + "/script-parameters/naDetectionCloseZOIBoundaryThresholdMillimeters", params.getPayload().getParameterAsDouble("na-detection-close-boundary-threshold", 1.5));
        parameterOverrides.put(nodeId + "/script-parameters/naDetectionContrastThreshold", params.getPayload().getParameterAsDouble("na-detection-contrast-threshold", 0.44));
        parameterOverrides.put(nodeId + "/script-parameters/naDetectionMaxCoefficientOfVariationAtHigh", params.getPayload().getParameterAsDouble("na-detection-max-cov-at-high", 0.15));
        parameterOverrides.put(nodeId + "/script-parameters/ringWidthMillimeters", params.getPayload().getParameterAsDouble("ring-width", 0.3));
        parameterOverrides.put(nodeId + "/script-parameters/ringWidthMaxMillimeters", params.getPayload().getParameterAsDouble("ring-width-max", 3));
        parameterOverrides.put(nodeId + "/script-parameters/scaleFactor", params.getPayload().getParameterAsDouble("scale-factor", 1));
        parameterOverrides.put(nodeId + "/script-parameters/zoiBoundaryDetectionLawnFraction", params.getPayload().getParameterAsDouble("zoi-boundary-lawn-fraction", 0.3));
        parameterOverrides.put(nodeId + "/script-parameters/zoiBoundaryDetectionMinInhibitionThresholdMillimeters", params.getPayload().getParameterAsDouble("zoi-boundary-min-inhibition-threshold", 0.5));
        parameterOverrides.put(nodeId + "/script-parameters/zoiBoundaryWideThresholdMillimeters", params.getPayload().getParameterAsDouble("zoi-boundary-wide-threshold", 7.5));
        parameterOverrides.put(nodeId + "/script-parameters/enableNADetection", params.getPayload().getParameterAsBoolean("enable-na-detection", true));
        parameterOverrides.put(nodeId + "/script-parameters/enableInverseDetection", withInvertedImages);

        Path projectFilePath = taskUtils.writeSharedFile(params, Path.of("workflows", "mic-analysis.jip"));

        progressInfo.log("Project file is " + projectFilePath);
        taskUtils.runJIPipe(params, projectFilePath, parameterOverrides, "", progressInfo, systemPackages, preferSystemPackages, verbose);

        String resultName = StringUtils.orElse(params.getPayload().getParameter("result-name").getValue(), "Visualization");
        String resultDescription = StringUtils.nullToEmpty(params.getPayload().getParameter("result-description").getValue());
        Project project = projectRepository.findById(params.getPayload().getProjectId()).get();

        taskUtils.readResultsDirectory(resultName, resultDescription, params.getTmpPath().resolve("results"), project, projectRepository, fileStorageService, Predicates.alwaysTrue(), progressInfo, verbose);

        if(params.getPayload().getParameterAsBoolean("write-mic", true)) {
            List<Map<String, String>> updatedMetadata = taskUtils.readCsv(params,  params.getTmpPath().resolve("results").resolve("mic_results.csv"));
            List<Image> toSave = new ArrayList<>();
            for (Map<String, String> map : updatedMetadata) {
                String imageId = map.get("#ImageId");
                String mic = map.get("MIC");
                if (imageId != null && mic != null && !mic.equalsIgnoreCase("N/A")) {
                    long imageId_ = Long.parseLong(imageId);
                    double mic_;
                    try {
                        mic_ = Double.parseDouble(mic);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    Optional<Image> image = imageRepository.findById(imageId_);
                    if (image.isPresent()) {
                        image.get().setMic(mic_);
                        toSave.add(image.get());
                    }
                }
            }

            imageRepository.saveAll(toSave);
        }
    }
}
