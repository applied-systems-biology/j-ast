package org.hkijena.jast.tasks.workloads.analyze;

import com.google.common.base.Predicates;
import jakarta.transaction.Transactional;
import org.hkijena.jast.config.SystemPackage;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.ViewMode;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.repositories.ResultRepository;
import org.hkijena.jast.services.BackendTaskRegistry;
import org.hkijena.jast.services.BackendTaskUtils;
import org.hkijena.jast.services.FileStorageService;
import org.hkijena.jast.tasks.*;
import org.hkijena.jast.utils.JASTDataSlot;
import org.hkijena.jast.utils.ProgressInfo;
import org.hkijena.jast.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Advanced, "bgr-subtraction-start-radius", "Background subtraction start radius (px)", "The lowest background subtraction radius", 20),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Advanced, "bgr-subtraction-end-radius", "Background subtraction end radius (px)", "The highest background subtraction radius", 64),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Advanced, "bgr-subtraction-radius-increment", "Background subtraction radius increment (px)", "Increment for the background subtraction radius sweep", 4),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Common, "confidence-threshold", "Confidence threshold (%)", "The minimum confidence for the OCR algorithm results", 90),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Advanced, "target-dpi", "Target DPI", "The target DPI for image upscaling. OCR algorithms require a high DPI to work properly.", 600),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Common, "strip-safety-distance", "Exclusion distance around strip (mm)", "The area around the strip annotation that is excluded from the measurement process. Must be large enough to exclude the strip itself.", 1),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Number, BackendTaskWorkloadParameterSlotType.Common, "strip-measure-distance", "Measurement distance around strip (mm)", "The area around the strip annotation that is used for the measurement process.", 10),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.Boolean, BackendTaskWorkloadParameterSlotType.Advanced, "add-inverted-candidates", "Generate inverted OCR inputs (slow)", "If enabled, create also inverted images for OCR processing. Doubles the required processing time", false)
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
        return "Auto-detect MIC";
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

        parameterOverrides.put("/bgrSubtractionStartRadius", params.getPayload().getParameterAsDouble("bgr-subtraction-start-radius", 20));
        parameterOverrides.put("/bgrSubtractionEndRadius", params.getPayload().getParameterAsDouble("bgr-subtraction-end-radius", 64));
        parameterOverrides.put("/bgrSubtractionRadiusIncrement", params.getPayload().getParameterAsDouble("bgr-subtraction-radius-increment", 4));
        parameterOverrides.put("/confidenceThreshold", params.getPayload().getParameterAsDouble("confidence-threshold", 90));
        parameterOverrides.put("/targetDPI", params.getPayload().getParameterAsDouble("target-dpi", 600));
        parameterOverrides.put("/stripSafetyDistanceMillimeters", params.getPayload().getParameterAsDouble("strip-safety-distance", 1));
        parameterOverrides.put("/stripMeasurementDistanceMillimeters", params.getPayload().getParameterAsDouble("strip-measure-distance", 10));
        parameterOverrides.put("/handleInvertedImages", params.getPayload().getParameterAsBoolean("add-inverted-candidates", false));

        Path projectFilePath = taskUtils.writeSharedFile(params, Path.of("workflows", "read-mic.jip"));

        progressInfo.log("Project file is " + projectFilePath);
        taskUtils.runJIPipe(params, projectFilePath, parameterOverrides, "", progressInfo, systemPackages, preferSystemPackages, verbose);

        String resultName = StringUtils.orElse(params.getPayload().getParameter("result-name").getValue(), "Visualization");
        String resultDescription = StringUtils.nullToEmpty(params.getPayload().getParameter("result-description").getValue());
        Project project = projectRepository.findById(params.getPayload().getProjectId()).get();

        taskUtils.readResultsDirectory(resultName, resultDescription, params.getTmpPath().resolve("results"), project, projectRepository, fileStorageService, Predicates.alwaysTrue(), progressInfo, verbose);
    }
}
