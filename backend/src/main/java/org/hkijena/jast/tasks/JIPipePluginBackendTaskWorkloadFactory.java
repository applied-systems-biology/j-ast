package org.hkijena.jast.tasks;

import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.ViewMode;
import org.hkijena.jast.tasks.workloads.JIPipePluginBackendTaskWorkload;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class JIPipePluginBackendTaskWorkloadFactory {
    private final ObjectProvider<JIPipePluginBackendTaskWorkload> taskProvider;

    @Autowired
    public JIPipePluginBackendTaskWorkloadFactory(ObjectProvider<JIPipePluginBackendTaskWorkload> taskProvider) {
        this.taskProvider = taskProvider;
    }

    public JIPipePluginBackendTaskWorkload create(Path pluginFile,
                                                  String name,
                                                  String description,
                                                  String category,
                                                  BackendTaskWorkloadMode mode,
                                                  List<BackendTaskWorkloadDataSlot> inputs,
                                                  List<BackendTaskWorkloadDataSlot> outputs,
                                                  List<BackendTaskWorkloadParameterSlot> parameters,
                                                  AssayType assayTypeRestriction,
                                                  ViewMode viewModeRestriction,
                                                  boolean generatesResults) {
        JIPipePluginBackendTaskWorkload workload = taskProvider.getObject();
        workload.setPluginFile(pluginFile);
        workload.setName(name);
        workload.setDescription(description);
        workload.setCategory(category);
        workload.setMode(mode);
        workload.setInputs(inputs);
        workload.setOutputs(outputs);
        workload.setParameters(parameters);
        workload.setAssayTypeRestriction(assayTypeRestriction);
        workload.setViewModeRestriction(viewModeRestriction);
        workload.setOutputsResult(generatesResults);
        return workload;
    }
}
