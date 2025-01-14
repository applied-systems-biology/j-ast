package org.hkijena.jast.tasks.workloads;

import jakarta.transaction.Transactional;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.payloads.task.BackendTaskParameterPayload;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.tasks.*;
import org.hkijena.jast.utils.JASTAnnotation;
import org.hkijena.jast.utils.ProgressInfo;
import org.hkijena.jast.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@BackendTaskType(typeId = "j-ast-analysis")
public class JASTAnalysisWorkload implements BackendTaskWorkload {

    private final ImageRepository imageRepository;
    private final ProjectRepository projectRepository;
    private static final List<BackendTaskWorkloadDataSlot> INPUTS = List.of(
            JASTAnnotation.Plate.toSlot(),
            JASTAnnotation.StripDisk.toSlot(),
            JASTAnnotation.ZOIShape.toSlot(BackendTaskWorkloadDataSlotValidationMode.OncePerRow));
    private static final List<BackendTaskWorkloadDataSlot> OUTPUTS = Collections.emptyList();
    private static final List<BackendTaskWorkloadParameterSlot> PARAMETERS = List.of(
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.String, "result-name", "Result name", "The name of the generated result folder", "Visualization"),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotType.String, "result-description", "Result description", "Description of the generated result", "")
    );
    private static final Map<String, String> PARAMETER_OVERRIDES = new HashMap<>();

    @Autowired
    public JASTAnalysisWorkload(ImageRepository imageRepository, ProjectRepository projectRepository) {
        this.imageRepository = imageRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    public String getName() {
        return "J-AST analysis";
    }

    @Override
    public String getDescription() {
        return "Generates results with the J-AST analysis algorithm. Supports only two time points per row.";
    }

    @Override
    public BackendTaskWorkloadMode getMode() {
        return BackendTaskWorkloadMode.FullRow;
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
        writeRawImages(params, params.getPayload().getImageIds(), imageRepository, progressInfo);
        writeMaskAnnotations(params, params.getPayload().getImageIds(), "plate", imageRepository, progressInfo);
        writeMaskAnnotations(params, params.getPayload().getImageIds(), "strip-disk", imageRepository, progressInfo);
        writeRowFirstMaskAnnotations(params, params.getPayload().getImageIds(), "zoi-shape", imageRepository, progressInfo);

        Map<String, Object> parameterOverrides = new HashMap<>();
        for (BackendTaskParameterPayload parameter : params.getPayload().getParameters()) {
            String overriddenKey = PARAMETER_OVERRIDES.get(parameter.getId());
            if(overriddenKey != null) {
                parameterOverrides.put(overriddenKey, parameter.getValue());
            }
        }

//        Path projectFilePath = writeSharedFile(params, "etest-copy-registered-zoi-shape.jip");
//        progressInfo.log("Project file is " + projectFilePath);
//        runJIPipe(params, projectFilePath, parameterOverrides, "", progressInfo);
//
//        Map<String, Path> maskAnnotationsConfig = new HashMap<>();
//        maskAnnotationsConfig.put("zoi-shape", params.getTmpPath().resolve("zoi-shape-aligned"));
//        readMaskAnnotations(params.getPayload().getImageIds(), maskAnnotationsConfig, imageRepository, progressInfo);

        String resultName = StringUtils.orElse(params.getPayload().getParameter("result-name").getValue(), "Visualization");
        String resultDescription = StringUtils.nullToEmpty(params.getPayload().getParameter("result-description").getValue());
        Project project = projectRepository.findById(params.getPayload().getProjectId()).get();
    }
}
