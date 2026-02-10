package org.hkijena.jast.tasks.workloads.utils;

import jakarta.transaction.Transactional;
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

import java.util.Collections;
import java.util.List;

@Component
@BackendTaskType(typeId = "export-custom")
public class ExportForCustomWorkload implements BackendTaskWorkload {

    private static final List<BackendTaskWorkloadDataSlot> INPUTS = List.of(JASTDataSlot.Plate.toSlot(BackendTaskWorkloadDataSlotValidationMode.Optional),
            JASTDataSlot.ZOIShape.toSlot(BackendTaskWorkloadDataSlotValidationMode.Optional),
            JASTDataSlot.StripDisk.toSlot(BackendTaskWorkloadDataSlotValidationMode.Optional));
    private static final List<BackendTaskWorkloadDataSlot> OUTPUTS = Collections.emptyList();
    private static final List<BackendTaskWorkloadParameterSlot> PARAMETERS = List.of(
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.String, BackendTaskWorkloadParameterSlotType.Common, "result-name", "Result name", "The name of the generated result folder", "Exported"),
            new BackendTaskWorkloadParameterSlot(BackendTaskWorkloadParameterSlotDataType.String, BackendTaskWorkloadParameterSlotType.Common, "result-description", "Result description", "Description of the generated result", "")
    );

    private final ImageRepository imageRepository;
    private final ResultRepository resultRepository;
    private final ProjectRepository projectRepository;
    private final FileStorageService fileStorageService;
    private final BackendTaskUtils taskUtils;
    private BackendTaskRegistry registry;

    @Autowired
    public ExportForCustomWorkload(ImageRepository imageRepository, ResultRepository resultRepository, ProjectRepository projectRepository, FileStorageService fileStorageService, BackendTaskUtils taskUtils) {
        this.imageRepository = imageRepository;
        this.resultRepository = resultRepository;
        this.projectRepository = projectRepository;
        this.fileStorageService = fileStorageService;
        this.taskUtils = taskUtils;
    }

    @Override
    public void setRegistry(@Lazy BackendTaskRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "Export for custom pipelines";
    }

    @Override
    public String getDescription() {
        return "Exports the selected images to be used in custom pipelines (through results)";
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
        return "Miscellaneous";
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
    public ViewMode getViewModeRestriction() {
        return null;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void execute(BackendTaskWorkloadParams params, ProgressInfo progressInfo) throws Throwable {
        final boolean verbose = params.getRuntimeConfig().isVerbose();

        taskUtils.writeRawImages(params, params.getPayload().getImageIds(), imageRepository, fileStorageService, progressInfo, verbose);
        taskUtils.writeMetadata(params, params.getPayload().getImageIds(), imageRepository, progressInfo, verbose);
        taskUtils.writeMaskAnnotations(params, params.getPayload().getImageIds(), "strip-disk", imageRepository, fileStorageService, progressInfo, verbose);
        taskUtils.writeMaskAnnotations(params, params.getPayload().getImageIds(), "zoi-shape", imageRepository, fileStorageService, progressInfo, verbose);
        taskUtils.writeMaskAnnotations(params, params.getPayload().getImageIds(), "plate", imageRepository, fileStorageService, progressInfo, verbose);

        String resultName = StringUtils.orElse(params.getPayload().getParameter("result-name").getValue(), "Exported");
        String resultDescription = StringUtils.nullToEmpty(params.getPayload().getParameter("result-description").getValue());
        Project project = projectRepository.findById(params.getPayload().getProjectId()).get();

        // We just grab the current directory
        taskUtils.readResultsDirectory(resultName, resultDescription, params.getTmpPath(), project, projectRepository, fileStorageService, (path) -> switch (path.getFileName().toString()) {
            case "lockfile", "log.txt", "job_started" -> false;
            default -> true;
        }, progressInfo, verbose);
    }
}
