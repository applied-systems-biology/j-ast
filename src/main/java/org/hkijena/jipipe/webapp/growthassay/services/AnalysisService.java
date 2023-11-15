package org.hkijena.jipipe.webapp.growthassay.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.eventbus.Subscribe;
import jakarta.transaction.Transactional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.Hibernate;
import org.hkijena.jipipe.webapp.growthassay.config.RuntimeConfig;
import org.hkijena.jipipe.webapp.growthassay.config.RuntimeParametersConfig;
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
import java.util.Optional;

@Service
public class AnalysisService {

    private final RuntimeConfig runtimeConfig;

    private final RuntimeParametersConfig runtimeParametersConfig;
    private final DatasetRepository datasetRepository;

    private final InputDataRepository inputDataRepository;
    private final OutputDataRepository outputDataRepository;

    @Autowired
    public AnalysisService(RuntimeConfig runtimeConfig, RuntimeParametersConfig runtimeParametersConfig, DatasetRepository datasetRepository, InputDataRepository inputDataRepository, OutputDataRepository outputDataRepository) {
        this.runtimeConfig = runtimeConfig;
        this.runtimeParametersConfig = runtimeParametersConfig;
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

            // Create a lockfile because interrupting the thread is not reliable
            Path lockFilePath = Path.of(dataset.getStoragePath()).resolve("lockfile");

            try {

                // Ensure that the lockfile exists
                if(!Files.isRegularFile(lockFilePath)) {
                    Files.createFile(lockFilePath);
                }

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

                if(!Files.isRegularFile(lockFilePath)) {
                    throw new InterruptedException();
                }

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

                // Save parameter config
                Path parameterOverridesFile = workDirectory.resolve("parameter-overrides.json");
                ObjectNode parameterOverrides = JsonUtils.getObjectMapper().createObjectNode();
                parameterOverrides.set(runtimeParametersConfig.getTimePointEarlyParameterKey(), new TextNode(dataset.getTimePointEarly()));
                parameterOverrides.set(runtimeParametersConfig.getGrowthReductionThresholdsParameterKey(), JsonUtils.getObjectMapper().convertValue(dataset.tryParseGrowthReductionThresholds(), JsonNode.class));
                parameterOverrides.set(runtimeParametersConfig.getDdaMinDiameterParameterKey(), new DoubleNode(dataset.getDdaDiskMinDiameter()));
                parameterOverrides.set(runtimeParametersConfig.getDdaMaxDiameterParameterKey(), new DoubleNode(dataset.getDdaDiskMaxDiameter()));
                parameterOverrides.set(runtimeParametersConfig.getDdaMinCircularityParameterKey(), new DoubleNode(dataset.getDdaDiskMinCircularity()));
                JsonUtils.saveToFile(parameterOverrides, parameterOverridesFile);

                // Run analysis
                ProgressInfo jipipeProgress = progressInfo.resolveAndLog("Running JIPipe");
                CommandLine commandLine;

                if(StringUtils.isNullOrEmpty(runtimeConfig.getFijiWrapper())) {
                    Path jipipeExecutablePath = Path.of(runtimeConfig.getFijiExecutablePath());
                    commandLine = new CommandLine(jipipeExecutablePath.toFile());
                }
                else {
                    Path jipipeExecutablePath = Path.of(runtimeConfig.getFijiExecutablePath());
                    commandLine = new CommandLine(Path.of(runtimeConfig.getFijiWrapper()).toFile());

                    for (String arg : runtimeConfig.getFijiWrapperArgs()) {
                        commandLine.addArgument(arg);
                    }

                    commandLine.addArgument(jipipeExecutablePath.toAbsolutePath().toString());
                }

                Path jipipeRootPath = Path.of(runtimeConfig.getFijiPath());
                progressInfo.incrementProgress();

                for (String arg : runtimeConfig.getFijiArgs()) {
                    commandLine.addArgument(arg);
                }


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
                commandLine.addArgument("--overwrite-parameters");
                commandLine.addArgument(parameterOverridesFile.toAbsolutePath().toString());

                ProcessUtils.ExtendedExecutor executor = new ProcessUtils.ExtendedExecutor(ExecuteWatchdog.INFINITE_TIMEOUT, jipipeProgress, lockFilePath);
                executor.setWorkingDirectory(jipipeRootPath.toFile());
                ProcessUtils.setupLogger(commandLine, executor, jipipeProgress);

                try {
                    executor.execute(commandLine);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if(!Files.isRegularFile(lockFilePath)) {
                    throw new InterruptedException();
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
                        double rad = NumberUtils.createDouble(record.get("RAD_mm"));
                        double threshold = NumberUtils.createDouble(record.get("#Threshold"));

                        outputData.setExperiment(experiment);
                        outputData.setSample(sample);
                        outputData.setFog(fog);
                        outputData.setRad(rad);
                        outputData.setThreshold(threshold * 100);
                        outputData.setInputData(inputDataRepository.findByDatasetAndExperimentAndSample(dataset, experiment, sample));

                        Path visualizationPath = workDirectory.resolve("results").resolve("visualizations").resolve("ZOI").resolve(experiment + "_" + sample + "_t" + threshold + ".png");
                        Path visualizationThumbnailPath = Files.createTempFile(resultServiceDirectory, "thumbnail", ".png");
                        ImageUtils.createThumbnail(ImageIO.read(visualizationPath.toFile()), 128, 64, visualizationThumbnailPath);
                        outputData.setVisualizationStoragePath(visualizationPath.toAbsolutePath().toString());
                        outputData.setVisualizationThumbnailStoragePath(visualizationThumbnailPath.toAbsolutePath().toString());

                        outputData = outputDataRepository.save(outputData);
                        dataset.addOutputData(outputData);
                    }
                }

                if(!Files.isRegularFile(lockFilePath)) {
                    throw new InterruptedException();
                }

                // ZIP analysis results
                Path zipFile = resultServiceDirectory.resolve("results.zip");
                ProgressInfo zipProgress = progressInfo.resolveAndLog("Compressing results");
                progressInfo.incrementProgress();
                ArchiveUtils.zipDirectory(workDirectory, StringUtils.makeFilesystemCompatible("" + dataset.getName()), zipFile, zipProgress);

                if(!Files.isRegularFile(lockFilePath)) {
                    throw new InterruptedException();
                }

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
            finally {
                try {
                    Files.deleteIfExists(lockFilePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
