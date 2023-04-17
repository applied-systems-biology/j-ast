package org.hkijena.jipipe.webapp.growthassay.services;

import org.hkijena.jipipe.webapp.growthassay.config.RuntimeConfig;
import org.hkijena.jipipe.webapp.growthassay.model.Dataset;
import org.hkijena.jipipe.webapp.growthassay.repositories.DatasetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnalysisService {

    private final RuntimeConfig runtimeConfig;
    private final DatasetRepository datasetRepository;

    @Autowired
    public AnalysisService(RuntimeConfig runtimeConfig, DatasetRepository datasetRepository) {
        this.runtimeConfig = runtimeConfig;
        this.datasetRepository = datasetRepository;
    }

    public void cleanupAllOrphanedRunningTasks() {
        datasetRepository.findAll().forEach(dataset -> {
            if(dataset.getStatus() == Dataset.Status.Running) {
                dataset.setStatus(Dataset.Status.RunInterrupted);
                datasetRepository.save(dataset);
            }
        });
    }
}
