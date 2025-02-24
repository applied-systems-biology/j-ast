package org.hkijena.jast.tasks;

import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.ViewMode;
import org.hkijena.jast.services.BackendTaskRegistry;
import org.hkijena.jast.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;

import java.util.*;

public interface BackendTaskWorkload {

    void setRegistry(@Lazy BackendTaskRegistry registry);

    String getName();

    default String getShortDescription() {
        return getDescription();
    }

    String getDescription();

    String getCategory();

    BackendTaskWorkloadMode getMode();

    List<BackendTaskWorkloadDataSlot> getInputs();

    List<BackendTaskWorkloadDataSlot> getOutputs();

    List<BackendTaskWorkloadParameterSlot> getParameters();

    AssayType getAssayTypeRestriction();

    ViewMode getViewModeRestriction();

    boolean isOutputsResult();

    void execute(BackendTaskWorkloadParams params, ProgressInfo progressInfo) throws Throwable;


}
