package org.hkijena.jipipe.webapp.growthassay.services;

import jakarta.transaction.Transactional;
import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hkijena.jipipe.webapp.growthassay.config.RuntimeConfig;
import org.hkijena.jipipe.webapp.growthassay.model.Dataset;
import org.hkijena.jipipe.webapp.growthassay.model.InputData;
import org.hkijena.jipipe.webapp.growthassay.repositories.DatasetRepository;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.context.JobDashboardLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
public class AnalysisService {

    private final RuntimeConfig runtimeConfig;
    private final DatasetRepository datasetRepository;

    @Autowired
    public AnalysisService(RuntimeConfig runtimeConfig, DatasetRepository datasetRepository) {
        this.runtimeConfig = runtimeConfig;
        this.datasetRepository = datasetRepository;
    }

    public void cleanupAllOrphanedRunningTasks(JobContext context) {
        datasetRepository.findAll().forEach(dataset -> {
            if(dataset.getStatus() == Dataset.Status.Running) {
                dataset.setStatus(Dataset.Status.RunInterrupted);
                datasetRepository.save(dataset);

                Path workDirectory = Paths.get(dataset.getStoragePath()).resolve("project");
                if(Files.isDirectory(workDirectory)) {
                    try {
                        FileSystemUtils.deleteRecursively(workDirectory);
                    } catch (IOException e) {
                        context.logger().error(e.toString());
                    }
                }
            }
        });
    }

    @Transactional
    public void runAnalysis(long datasetId, JobContext context) {
        JobDashboardLogger logger = context.logger();
        Optional<Dataset> dataset_ = datasetRepository.findById(datasetId);
        if(dataset_.isPresent()) {
            Dataset dataset = dataset_.get();
            Hibernate.initialize(dataset.getInputData());

            try {
                // Get & cleanup work directory
                logger.info("Creating and cleaning work directory ...");
                Path workDirectory = Paths.get(dataset.getStoragePath()).resolve("project");
                if(Files.isDirectory(workDirectory)) {
                    FileSystemUtils.deleteRecursively(workDirectory);
                }

                Files.createDirectories(workDirectory);
                Path inputDirectory = workDirectory.resolve("raw");
                Files.createDirectories(inputDirectory);

                // Copy inputs into the raw directory
                logger.info("Copying inputs ...");
                for (InputData data : dataset.getInputData()) {
                    Files.copy(Paths.get(data.getStoragePath()), inputDirectory.resolve(data.getFinalFileName() + ".png"));
                }

                throw new NotImplementedException();
            }
            catch (Throwable e) {
                dataset.setStatus(Dataset.Status.RunInterrupted);
                datasetRepository.save(dataset);
                e.printStackTrace();
                logger.error(e.toString());
            }
        }
    }
}
