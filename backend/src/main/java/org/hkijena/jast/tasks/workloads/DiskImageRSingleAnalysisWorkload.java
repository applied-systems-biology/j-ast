package org.hkijena.jast.tasks.workloads;

import com.google.common.base.Predicates;
import jakarta.transaction.Transactional;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.ViewMode;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.payloads.task.BackendTaskParameterPayload;
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
@BackendTaskType(typeId = "disk-image-r-analysis-single")
public class DiskImageRSingleAnalysisWorkload implements BackendTaskWorkload {

    private static final List<BackendTaskWorkloadDataSlot> INPUTS = List.of(
            JASTDataSlot.Plate.toSlot(),
            JASTDataSlot.StripDisk.toSlot(),
            JASTDataSlot.ZOIShape.toSlot(BackendTaskWorkloadDataSlotValidationMode.OncePerRow));
    private static final List<BackendTaskWorkloadDataSlot> OUTPUTS = Collections.emptyList();
    private static final List<BackendTaskWorkloadParameterSlot> PARAMETERS = List.of(
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.String, "result-name", "Result name", "The name of the generated result folder", "DiskImageR-style result (single)"),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.String, "result-description", "Result description", "Description of the generated result", "RAD/FoG/ZOI"),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.String, "thresholds", "Thresholds (%)", "The thresholds in percent (separate items with a semicolon)", "20; 50; 80"),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.Number, "global-plate-shave-off-mm", "Plate edge thickness (mm)", "The thickness of the plate edge, which is subtracted from the plate area. If not set appropriately, the measurements will be skewed by the bright plate edge.", 10),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.Boolean, "num-diff", "Numeric integration", "If enabled, use less accurate numeric integration for calculating the AUC. Otherwise, a function is fitted to the measurements, which may yield to crashes", true),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.Number, "global-label-thickness-mm", "Measurement label thickness (mm)", "The thickness of the measurement labels. A lower value increases the resolution, but also may introduce some additional noise.", 1)
    );
    private static final Map<String, String> PARAMETER_OVERRIDES = new HashMap<>();

    static {
        PARAMETER_OVERRIDES.put("__thresholds", "3d3aba6c-2688-47bd-94af-aef3ebf058bf/variables/thresholds");
    }

    private final ImageRepository imageRepository;
    private final ProjectRepository projectRepository;
    private final ResultRepository resultRepository;
    private final FileStorageService fileStorageService;
    private final BackendTaskUtils taskUtils;
    private BackendTaskRegistry registry;

    @Autowired
    public DiskImageRSingleAnalysisWorkload(ImageRepository imageRepository, ProjectRepository projectRepository, ResultRepository resultRepository, FileStorageService fileStorageService, BackendTaskUtils taskUtils) {
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
        return "DiskImageR-style analysis (RAD/FoG/ZOI) - Single images";
    }

    @Override
    public String getShortDescription() {
        return "Applies an analysis that is based on the DiskImageR tool (Gerstein et al).";
    }

    @Override
    public String getDescription() {
        return "Applies an analysis that is based on the DiskImageR tool (Gerstein et al). " +
                "For each image, the tool will generate label areas around the strip/disk that follows the zone-of-inhibition (ZOI) shape (i.e. for DDA this is trivial). " +
                "Then the analysis will proceed to find for each percentage threshold (default 20%, 50%, and 80%) the label that has the given reduction in brightness. " +
                "This yields the radius-of-inhibition (RAD), which for DDA is the distance of the ZOI border to the disk, and for E-tests the distance to the point inside the strip where the distance to the ZOI shape is greatest. " +
                "The ZOI is then applied to the other time point to calculate the field-of-growth (FoG)." +
                "Please note that the pixel size must be correctly calibrated for appropriate physical RAD values in millimeters.";
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
        return AssayType.Unknown;
    }

    @Override
    public boolean isOutputsResult() {
        return true;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void execute(BackendTaskWorkloadParams params, ProgressInfo progressInfo) throws Throwable {
        final boolean verbose = params.getRuntimeConfig().isVerbose();

        Map<String, Object> parameterOverrides = new HashMap<>();

        // Parse and read the thresholds
        String rawThresholds = StringUtils.nullToEmpty(params.getPayload().getParameter("thresholds").getValue());
        List<Integer> thresholds = StringUtils.getIntegersFromRangeString(rawThresholds);
        parameterOverrides.put(PARAMETER_OVERRIDES.get("__thresholds"), thresholds.stream().map(t -> t / 100.0).toList());

        taskUtils.writeRawImages(params, params.getPayload().getImageIds(), imageRepository, fileStorageService, progressInfo, verbose);
        taskUtils.writeMaskAnnotations(params, params.getPayload().getImageIds(), "plate", imageRepository, fileStorageService, progressInfo, verbose);
        taskUtils.writeMaskAnnotations(params, params.getPayload().getImageIds(), "strip-disk", imageRepository, fileStorageService, progressInfo, verbose);
        taskUtils.writeRowFirstMaskAnnotations(params, params.getPayload().getImageIds(), "zoi-shape", imageRepository, fileStorageService, progressInfo, verbose);

        for (BackendTaskParameterPayload parameter : params.getPayload().getParameters()) {
            String overriddenKey = PARAMETER_OVERRIDES.get(parameter.getId());
            if (overriddenKey != null) {
                parameterOverrides.put(overriddenKey, parameter.getValue());
            }
        }

        parameterOverrides.put("/plateShaveOffMillimeters", params.getPayload().getParameterAsDouble("global-plate-shave-off-mm", 10));
        parameterOverrides.put("/labelWidthMillimeters", params.getPayload().getParameterAsDouble("global-label-thickness-mm", 1));

        boolean useNumDiff = params.getPayload().getParameterAsBoolean("num-diff", true);

        Path projectFilePath = taskUtils.writeSharedFile(params, useNumDiff ? "disk-image-r-analysis-single-numdiff.jip" : "disk-image-r-analysis-single.jip");

        progressInfo.log("Project file is " + projectFilePath);
        taskUtils.runJIPipe(params, projectFilePath, parameterOverrides, "", progressInfo, verbose);

        String resultName = StringUtils.orElse(params.getPayload().getParameter("result-name").getValue(), "Visualization");
        String resultDescription = StringUtils.nullToEmpty(params.getPayload().getParameter("result-description").getValue());
        Project project = projectRepository.findById(params.getPayload().getProjectId()).get();

        taskUtils.readResultsDirectory(resultName, resultDescription, params.getTmpPath().resolve("results"), project, projectRepository, fileStorageService, Predicates.alwaysTrue(), progressInfo, verbose);
    }
}
