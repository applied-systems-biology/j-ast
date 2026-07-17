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

package org.hkijena.jast.tasks;

import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.ViewMode;
import org.hkijena.jast.services.BackendTaskRegistry;
import org.hkijena.jast.utils.ProgressInfo;
import org.springframework.context.annotation.Lazy;

import java.util.List;

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
