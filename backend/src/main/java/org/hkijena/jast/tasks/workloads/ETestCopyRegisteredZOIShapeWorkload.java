package org.hkijena.jast.tasks.workloads;

import jakarta.transaction.Transactional;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.payloads.task.BackendTaskParameterPayload;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.tasks.*;
import org.hkijena.jast.utils.JASTAnnotation;
import org.hkijena.jast.utils.ProgressInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@BackendTaskType(typeId = "etest-copy-registered-zoi-shape")
public class ETestCopyRegisteredZOIShapeWorkload implements BackendTaskWorkload {

    private final ImageRepository imageRepository;
    private static final List<BackendTaskWorkloadDataSlot> INPUTS = List.of(JASTAnnotation.StripDisk.toSlot(),
            JASTAnnotation.ZOIShape.toSlot(BackendTaskWorkloadDataSlotValidationMode.OncePerRow));
    private static final List<BackendTaskWorkloadDataSlot> OUTPUTS = Collections.emptyList();
    private static final List<BackendTaskWorkloadParameterSlot> PARAMETERS = Collections.emptyList();
    private static final Map<String, String> PARAMETER_OVERRIDES = new HashMap<>();

    @Autowired
    public ETestCopyRegisteredZOIShapeWorkload(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    @Override
    public String getName() {
        return "Copy and register ZOI shape across timeline";
    }

    @Override
    public String getDescription() {
        return "Copies the first available ZOI shape to the other images in the timeline. " +
                "The strip is used to register the ZOI shape image.";
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
        return "E-Test";
    }

    @Override
    public AssayType getAssayTypeRestriction() {
        return AssayType.ETest;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void execute(BackendTaskWorkloadParams params, ProgressInfo progressInfo) throws Throwable {
        writeRawImages(params, params.getPayload().getImageIds(), imageRepository, progressInfo);
        writeMaskAnnotations(params, params.getPayload().getImageIds(), "strip-disk", imageRepository, progressInfo);
        writeRowFirstMaskAnnotations(params, params.getPayload().getImageIds(), "zoi-shape", imageRepository, progressInfo);

        Map<String, Object> parameterOverrides = new HashMap<>();
        for (BackendTaskParameterPayload parameter : params.getPayload().getParameters()) {
            parameterOverrides.put(PARAMETER_OVERRIDES.get(parameter.getId()), parameter.getValue());
        }

        Path projectFilePath = writeSharedFile(params, "etest-copy-registered-zoi-shape.jip");
        progressInfo.log("Project file is " + projectFilePath);
        runJIPipe(params, projectFilePath, parameterOverrides, "", progressInfo);

        Map<String, Path> maskAnnotationsConfig = new HashMap<>();
        maskAnnotationsConfig.put("zoi-shape", params.getTmpPath().resolve("zoi-shape-aligned"));
        readMaskAnnotations(params.getPayload().getImageIds(), maskAnnotationsConfig, imageRepository, progressInfo);
    }
}
