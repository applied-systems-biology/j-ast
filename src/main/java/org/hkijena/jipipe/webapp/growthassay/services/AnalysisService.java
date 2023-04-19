package org.hkijena.jipipe.webapp.growthassay.services;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;
import org.hkijena.jipipe.webapp.growthassay.config.RuntimeConfig;
import org.hkijena.jipipe.webapp.growthassay.model.Dataset;
import org.hkijena.jipipe.webapp.growthassay.model.InputData;
import org.hkijena.jipipe.webapp.growthassay.repositories.DatasetRepository;
import org.jobrunr.jobs.context.JobContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

@Service
public class AnalysisService {

    private final RuntimeConfig runtimeConfig;
    private final DatasetRepository datasetRepository;

    private final EntityManager entityManager;

    @Autowired
    public AnalysisService(RuntimeConfig runtimeConfig, DatasetRepository datasetRepository, EntityManager entityManager) {
        this.runtimeConfig = runtimeConfig;
        this.datasetRepository = datasetRepository;
        this.entityManager = entityManager;
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

    public void logInfo(String message, JobContext context, Dataset dataset) {
        context.logger().info(message);
        Path logFilePath = Paths.get(dataset.getStoragePath()).resolve("log.txt");
        try {
            Files.writeString(logFilePath, "[INFO] " + message + "\n", StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logError(String message, JobContext context, Dataset dataset) {
        context.logger().error(message);
        Path logFilePath = Paths.get(dataset.getStoragePath()).resolve("log.txt");
        try {
            Files.writeString(logFilePath, "[ERROR] " + message + "\n", StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Transactional
    public void runAnalysis(long datasetId, JobContext context) {
        Optional<Dataset> dataset_ = datasetRepository.findById(datasetId);
        if(dataset_.isPresent()) {
            Dataset dataset = dataset_.get();
            Hibernate.initialize(dataset.getInputData());

            try {
                // Clean logs
                Path logFilePath = Paths.get(dataset.getStoragePath()).resolve("log.txt");
                Files.deleteIfExists(logFilePath);

                // Get & cleanup work directory
                logInfo("Creating and cleaning work directory ...", context, dataset);
                Path workDirectory = Paths.get(dataset.getStoragePath()).resolve("project");
                if(Files.isDirectory(workDirectory)) {
                    FileSystemUtils.deleteRecursively(workDirectory);
                }

                Files.createDirectories(workDirectory);
                Path inputDirectory = workDirectory.resolve("raw");
                Files.createDirectories(inputDirectory);

                // Copy inputs into the raw directory
                logInfo("Copying inputs ...", context, dataset);
                for (InputData data : dataset.getInputData()) {
                    Files.copy(Paths.get(data.getStoragePath()), inputDirectory.resolve(data.getFinalFileName() + ".png"));
                }

                // Extract project file

                // Run analysis

                // Generate thumbnails

                // ZIP analysis results

                // Finalize the analysis
                dataset.setStatus(Dataset.Status.RunFinished);
                datasetRepository.save(dataset);
            }
            catch (Throwable e) {
                dataset.setStatus(Dataset.Status.RunInterrupted);
                datasetRepository.save(dataset);
                e.printStackTrace();
                logError(e.toString(), context, dataset);
            }
        }
    }
}
