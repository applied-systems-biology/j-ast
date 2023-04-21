package org.hkijena.jipipe.webapp.growthassay.services;

import com.google.common.eventbus.Subscribe;
import jakarta.transaction.Transactional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.Hibernate;
import org.hkijena.jipipe.webapp.growthassay.config.RuntimeConfig;
import org.hkijena.jipipe.webapp.growthassay.model.Dataset;
import org.hkijena.jipipe.webapp.growthassay.model.InputData;
import org.hkijena.jipipe.webapp.growthassay.model.OutputData;
import org.hkijena.jipipe.webapp.growthassay.repositories.DatasetRepository;
import org.hkijena.jipipe.webapp.growthassay.repositories.InputDataRepository;
import org.hkijena.jipipe.webapp.growthassay.repositories.OutputDataRepository;
import org.hkijena.jipipe.webapp.growthassay.utils.*;
import org.jobrunr.jobs.context.JobContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import javax.imageio.ImageIO;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

@Service
public class AnalysisService {

    private final RuntimeConfig runtimeConfig;
    private final DatasetRepository datasetRepository;

    private final InputDataRepository inputDataRepository;
    private final OutputDataRepository outputDataRepository;

    @Autowired
    public AnalysisService(RuntimeConfig runtimeConfig, DatasetRepository datasetRepository, InputDataRepository inputDataRepository, OutputDataRepository outputDataRepository) {
        this.runtimeConfig = runtimeConfig;
        this.datasetRepository = datasetRepository;
        this.inputDataRepository = inputDataRepository;
        this.outputDataRepository = outputDataRepository;
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

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void runAnalysis(long datasetId, JobContext context) {
        Optional<Dataset> dataset_ = datasetRepository.findById(datasetId);
        if(dataset_.isPresent()) {
            Dataset dataset = dataset_.get();
            Hibernate.initialize(dataset.getInputData());

            try {
                // Save job ID because tx not working
                Files.writeString(Path.of(dataset.getStoragePath()).resolve("job-id.txt"), context.getJobId().toString(), StandardOpenOption.CREATE);

                // Clean logs
                Path logFilePath = Paths.get(dataset.getStoragePath()).resolve("log.txt");
                Files.deleteIfExists(logFilePath);

                ProgressInfo progressInfo = new ProgressInfo();
                progressInfo.setProgress(1,5);
                progressInfo.getEventBus().register(new Object() {
                    @Subscribe
                    public void onStatusUpdated(ProgressInfo.StatusUpdatedEvent event) {
                        logInfo( event.render(), context, dataset);
                    }
                });

                // Get & cleanup work directory
                progressInfo.log("Creating and cleaning work directory");
                progressInfo.incrementProgress();
                Path workDirectory = Paths.get(dataset.getStoragePath()).resolve("project");
                Path resultServiceDirectory = Paths.get(dataset.getStoragePath()).resolve("results");
                if(Files.isDirectory(workDirectory)) {
                    FileSystemUtils.deleteRecursively(workDirectory);
                }
                if(Files.isDirectory(resultServiceDirectory)) {
                    FileSystemUtils.deleteRecursively(resultServiceDirectory);
                }

                Files.createDirectories(workDirectory);
                Files.createDirectories(resultServiceDirectory);
                Path inputDirectory = workDirectory.resolve("raw");
                Path jipipeOutputDirectory = workDirectory.resolve("results-jipipe");
                Files.createDirectories(inputDirectory);

                // Copy inputs into the raw directory
                progressInfo.log("Copying inputs");
                progressInfo.incrementProgress();
                for (InputData data : dataset.getInputData()) {
                    Files.copy(Paths.get(data.getStoragePath()), inputDirectory.resolve(data.getFinalFileName() + ".png"));
                }

                // Extract project file
                Path projectFile = workDirectory.resolve("project.jip");
                Files.copy(new ClassPathResource("jipipe/project.jip").getInputStream(),
                        projectFile);

                // Run analysis
                ProgressInfo jipipeProgress = progressInfo.resolveAndLog("Running JIPipe");
                Path jipipeExecutablePath = Path.of(runtimeConfig.getFijiExecutablePath());
                Path jipipeRootPath = Path.of(runtimeConfig.getFijiPath());
                progressInfo.incrementProgress();

                CommandLine commandLine = new CommandLine(jipipeExecutablePath.toFile());
                commandLine.addArgument("--pass-classpath");
                commandLine.addArgument("--full-classpath");
                commandLine.addArgument("--main-class");
                commandLine.addArgument("org.hkijena.jipipe.JIPipeCLI");
                commandLine.addArgument("run");
                commandLine.addArgument("--project");
                commandLine.addArgument(projectFile.toAbsolutePath().toString());
                commandLine.addArgument("--output-folder");
                commandLine.addArgument(jipipeOutputDirectory.toAbsolutePath().toString());
                commandLine.addArgument("--output-results");
                commandLine.addArgument("only-compartment-outputs");

                ProcessUtils.ExtendedExecutor executor = new ProcessUtils.ExtendedExecutor(ExecuteWatchdog.INFINITE_TIMEOUT, jipipeProgress);
                executor.setWorkingDirectory(jipipeRootPath.toFile());
                ProcessUtils.setupLogger(commandLine, executor, jipipeProgress);

                try {
                    executor.execute(commandLine);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // Postprocessing results
                progressInfo.log("Postprocessing results");
                progressInfo.incrementProgress();
                dataset.clearOutputData();

                Path resultsAllInOneFile = workDirectory.resolve("results").resolve("results_all_in_one.csv");
                CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
                try(FileReader reader = new FileReader(resultsAllInOneFile.toFile())) {
                    for (CSVRecord record : csvFormat.parse(reader)) {
                        OutputData outputData = new OutputData();

                        String experiment = record.get("Experiment");
                        String sample = record.get("Sample");
                        double fog = NumberUtils.createDouble(record.get("FoG"));

                        outputData.setFog(fog);
                        outputData.setInputData(inputDataRepository.findByDatasetAndExperimentAndSample(dataset, experiment, sample));

                        Path visualizationPath = workDirectory.resolve("results").resolve("visualizations").resolve("ZOI").resolve(experiment + "_" + sample + ".png");
                        Path visualizationThumbnailPath = Files.createTempFile(resultServiceDirectory, "thumbnail", ".png");
                        ImageUtils.createThumbnail(ImageIO.read(visualizationPath.toFile()), visualizationThumbnailPath);
                        outputData.setVisualizationStoragePath(visualizationPath.toAbsolutePath().toString());
                        outputData.setVisualizationThumbnailStoragePath(visualizationThumbnailPath.toAbsolutePath().toString());

                        outputData = outputDataRepository.save(outputData);
                        dataset.addOutputData(outputData);
                    }
                }


                // ZIP analysis results
                Path zipFile = resultServiceDirectory.resolve("results.zip");
                ProgressInfo zipProgress = progressInfo.resolveAndLog("Compressing results");
                progressInfo.incrementProgress();
                ArchiveUtils.zipDirectory(workDirectory, StringUtils.makeFilesystemCompatible("" + dataset.getName()), zipFile, zipProgress);

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
