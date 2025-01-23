package org.hkijena.jast.services;

import com.google.common.eventbus.Subscribe;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jast.config.RuntimeConfig;
import org.hkijena.jast.model.TaskStatus;
import org.hkijena.jast.model.entities.BackendTask;
import org.hkijena.jast.payloads.task.BackendTaskPayload;
import org.hkijena.jast.repositories.BackendTaskRepository;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.tasks.BackendTaskWorkload;
import org.hkijena.jast.tasks.BackendTaskWorkloadParams;
import org.hkijena.jast.utils.PathUtils;
import org.hkijena.jast.utils.ProgressInfo;
import org.hkijena.jast.utils.StringUtils;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.scheduling.JobBuilder;
import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BackendTaskService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final RuntimeConfig runtimeConfig;
    private final ImageRepository imageRepository;
    private final ProjectRepository projectRepository;
    private final BackendTaskRegistry backendTaskRegistry;
    private final BackendTaskRepository backendTaskRepository;
    private final JobScheduler jobScheduler;

    @Autowired
    public BackendTaskService(RuntimeConfig runtimeConfig, ImageRepository imageRepository, ProjectRepository projectRepository, BackendTaskRegistry backendTaskRegistry, BackendTaskRepository backendTaskRepository, JobScheduler jobScheduler) {
        this.runtimeConfig = runtimeConfig;
        this.projectRepository = projectRepository;
        this.imageRepository = imageRepository;
        this.backendTaskRegistry = backendTaskRegistry;
        this.backendTaskRepository = backendTaskRepository;
        this.jobScheduler = jobScheduler;
    }

//    @Recurring(id = "start-scheduled-tasks", cron = "*/5 * * * * *")
//    @Job(name = "Start scheduled backend tasks")
//    @Transactional(Transactional.TxType.REQUIRES_NEW)
//    public void startTasks() {
//        List<BackendTask> newTasks = backendTaskRepository.findAllByStatus(TaskStatus.Ready);
//        int numFailures = 0;
//        for (BackendTask newTask : newTasks) {
//            BackendTaskWorkload workload = backendTaskRegistry.getTask(newTask.getTaskTypeId());
//            if (workload != null) {
//                BackendTaskPayload payload = newTask.toPayload();
//                newTask.setStatus(TaskStatus.Running);
//
//                BackendTaskWorkloadParams params = new BackendTaskWorkloadParams();
//                params.setPayload(payload);
//                params.setTmpPath(Paths.get(newTask.getTmpPath()));
//                params.setRuntimeConfig(runtimeConfig);
//                params.setLockFilePath(params.getTmpPath().resolve("lockfile"));
//                PathUtils.createFileIfNotExists(params.getLockFilePath());
//
//                jobScheduler.enqueue(() -> startBackendTask(params, JobContext.Null));
//            } else {
//                ++numFailures;
//            }
//        }
//        if (!newTasks.isEmpty()) {
//            logger.info("Started {} backend tasks, {} failures", newTasks.size(), numFailures);
//            backendTaskRepository.saveAll(newTasks);
//        }
//    }

    @Transactional
    public void enqueueTask(BackendTask newTask) {
        if (newTask.getStatus() != TaskStatus.Ready) {
            return;
        }
        BackendTaskWorkload workload = backendTaskRegistry.getTask(newTask.getTaskTypeId());
        if (workload != null) {
            BackendTaskPayload payload = newTask.toPayload();
            newTask.setStatus(TaskStatus.Running);

            BackendTaskWorkloadParams params = new BackendTaskWorkloadParams();
            params.setPayload(payload);
            params.setTmpPath(Paths.get(newTask.getTmpPath()));
            params.setRuntimeConfig(runtimeConfig);
            params.setLockFilePath(params.getTmpPath().resolve("lockfile"));
            PathUtils.createFileIfNotExists(params.getLockFilePath());

            jobScheduler.create(JobBuilder.aJob().withLabels("taskId-" + newTask.getId(), "projectId-" + newTask.getProject().getId()).withDetails(() -> startBackendTask(params, JobContext.Null)));
        }
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void startBackendTask(BackendTaskWorkloadParams params, JobContext jobContext) {
        BackendTaskPayload payload = params.getPayload();
        long taskId = payload.getId();
        BackendTaskWorkload workload = backendTaskRegistry.getTask(payload.getTaskId());

        Path logFilePath = params.getTmpPath().resolve("log.txt");
        ProgressInfo progressInfo = new ProgressInfo();
        progressInfo.setLogToStdOut(true);
        progressInfo.getEventBus().register(new Object() {
            @Subscribe
            public void onStatusUpdated(ProgressInfo.StatusUpdatedEvent event) {
                logInfo(event.render(), logFilePath, jobContext);
            }
        });

        try {
            workload.execute(params, progressInfo);
            progressInfo.log("Task execution successful");

            // Mark as successful
            Optional<BackendTask> task_ = backendTaskRepository.findById(taskId);
            if (task_.isPresent()) {
                BackendTask task = task_.get();
                task.setStatus(TaskStatus.Successful);
                task.setLog(progressInfo.getLog().toString());
                backendTaskRepository.save(task);
            }
        } catch (Throwable e) {
            logger.error("Error during task execution", e);
            logError(ExceptionUtils.getStackTrace(e), logFilePath, jobContext);

            // Mark as failed
            Optional<BackendTask> task_ = backendTaskRepository.findById(taskId);
            if (task_.isPresent()) {
                BackendTask task = task_.get();
                task.setStatus(TaskStatus.Failed);
                task.setLog(progressInfo.getLog().toString());
                backendTaskRepository.save(task);
            }
        }
    }

    @PostConstruct
    public void setAllTasksToFailed() {
        List<BackendTask> changedTasks = new ArrayList<>();
        for (BackendTask task : backendTaskRepository.findAll()) {
            if (task.isRunning()) {
                task.setStatus(TaskStatus.Failed);
                changedTasks.add(task);
                deleteTaskTmpPath(task);
            } else if (task.getStatus() == TaskStatus.Failed) {
                deleteTaskTmpPath(task);
            }
        }
        backendTaskRepository.saveAll(changedTasks);
        logger.info("Cleaned up {} tasks", changedTasks.size());
    }

    public void deleteTaskTmpPath(BackendTask task) {
        // Delete tmp path
        if (!StringUtils.isNullOrEmpty(task.getTmpPath())) {
            Path tmpPath = Paths.get(task.getTmpPath());
            if (Files.isDirectory(tmpPath)) {
                logger.warn("Deleting temporary directory {}", tmpPath);
                try {
                    FileSystemUtils.deleteRecursively(tmpPath.toFile());
                } catch (Exception e) {
                    logger.error("Failed to delete temporary directory {}", tmpPath, e);
                }
            }
        }
    }

    public Path createTmpPath() {
        Path result;
        if (!StringUtils.isNullOrEmpty(runtimeConfig.getCustomTempDirectory())) {
            try {
                Files.createDirectories(Paths.get(runtimeConfig.getCustomTempDirectory()));
                result = Files.createTempDirectory(Paths.get(runtimeConfig.getCustomTempDirectory()), "j-ast");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                result = Files.createTempDirectory("j-ast");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return PathUtils.createDirectories(result);
    }


    public void logInfo(String message, Path logFilePath, JobContext context) {
        context.logger().info(message);
        try {
            Files.writeString(logFilePath, "[INFO] " + message + "\n", StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logError(String message, Path logFilePath, JobContext context) {
        context.logger().error(message);
        try {
            Files.writeString(logFilePath, "[ERROR] " + message + "\n", StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//
//    @Transactional(Transactional.TxType.REQUIRES_NEW)
//    public void runAnalysis(long datasetId, JobContext context) {
//        Optional<TimeSeries> dataset_ = timeSeriesRepository.findById(datasetId);
//        if(dataset_.isPresent()) {
//            TimeSeries timeSeries = dataset_.get();
//            Hibernate.initialize(timeSeries.getImages());
//
//            // Create a lockfile because interrupting the thread is not reliable
//            Path lockFilePath = Path.of(timeSeries.getStoragePath()).resolve("lockfile");
//
//            try {
//
//                // Ensure that the lockfile exists
//                if(!Files.isRegularFile(lockFilePath)) {
//                    Files.createFile(lockFilePath);
//                }
//
//                // Save job ID because tx not working
//                Files.writeString(Path.of(timeSeries.getStoragePath()).resolve("job-id.txt"), context.getJobId().toString(), StandardOpenOption.CREATE);
//
//                // Clean logs
//                Path logFilePath = Paths.get(timeSeries.getStoragePath()).resolve("log.txt");
//                Files.deleteIfExists(logFilePath);
//
//                ProgressInfo progressInfo = new ProgressInfo();
//                progressInfo.setProgress(1,5);
//                progressInfo.getEventBus().register(new Object() {
//                    @Subscribe
//                    public void onStatusUpdated(ProgressInfo.StatusUpdatedEvent event) {
//                        logInfo( event.render(), context, timeSeries);
//                    }
//                });
//
//                // Get & cleanup work directory
//                progressInfo.log("Creating and cleaning work directory");
//                progressInfo.incrementProgress();
//                Path workDirectory = Paths.get(timeSeries.getStoragePath()).resolve("project");
//                Path resultServiceDirectory = Paths.get(timeSeries.getStoragePath()).resolve("results");
//                if(Files.isDirectory(workDirectory)) {
//                    FileSystemUtils.deleteRecursively(workDirectory);
//                }
//                if(Files.isDirectory(resultServiceDirectory)) {
//                    FileSystemUtils.deleteRecursively(resultServiceDirectory);
//                }
//
//                Files.createDirectories(workDirectory);
//                Files.createDirectories(resultServiceDirectory);
//                Path inputDirectory = workDirectory.resolve("raw");
//                Path jipipeOutputDirectory = workDirectory.resolve("results-jipipe");
//                Files.createDirectories(inputDirectory);
//
//                if(!Files.isRegularFile(lockFilePath)) {
//                    throw new InterruptedException();
//                }
//
//                // Copy inputs into the raw directory
//                progressInfo.log("Copying inputs");
//                progressInfo.incrementProgress();
//                for (Image data : timeSeries.getImages()) {
//                    Files.copy(Paths.get(data.getStoragePath()), inputDirectory.resolve(data.getFinalFileName() + ".png"));
//                }
//
//                // Extract project file
//                Path projectFile = workDirectory.resolve("project.jip");
//                Files.copy(new ClassPathResource("jipipe/project.jip").getInputStream(),
//                        projectFile);
//
//                // Save parameter config
//                Path parameterOverridesFile = workDirectory.resolve("parameter-overrides.json");
//                ObjectNode parameterOverrides = JsonUtils.getObjectMapper().createObjectNode();
//                parameterOverrides.set(runtimeParametersConfig.getInputFolderListParameterKey(), JsonUtils.readFromString("[\"raw\"]", JsonNode.class));
//                Map<String, String> timePointFilterConfig = new HashMap<>();
//                timePointFilterConfig.put("expression", "#Timepoint == \"" + timeSeries.getTimePointEarly() + "\"");
//                parameterOverrides.set(runtimeParametersConfig.getTimePointEarlyFilterParameterKey(), JsonUtils.getObjectMapper().convertValue(timePointFilterConfig, JsonNode.class));
//                parameterOverrides.set(runtimeParametersConfig.getGrowthReductionThresholdsParameterKey(), JsonUtils.getObjectMapper()
//                        .convertValue(timeSeries.tryParseGrowthReductionThresholds().stream().map(d -> d / 100.0).collect(Collectors.toList()), JsonNode.class));
//                parameterOverrides.set(runtimeParametersConfig.getDdaMinDiameterParameterKey(), new DoubleNode(timeSeries.getDdaDiskMinDiameter()));
//                parameterOverrides.set(runtimeParametersConfig.getDdaMaxDiameterParameterKey(), new DoubleNode(timeSeries.getDdaDiskMaxDiameter()));
//                parameterOverrides.set(runtimeParametersConfig.getDdaMinCircularityParameterKey(), new DoubleNode(timeSeries.getDdaDiskMinCircularity()));
//                parameterOverrides.set(runtimeParametersConfig.getContrastMinValueParameterKey(), new DoubleNode(timeSeries.getContrastMinValue()));
//                parameterOverrides.set(runtimeParametersConfig.getContrastMaxValueParameterKey(), new DoubleNode(timeSeries.getContrastMaxValue()));
//                parameterOverrides.set(runtimeParametersConfig.getEnsureCircularPlateParameterKey(), timeSeries.isEnsureCircularPlate() ? BooleanNode.TRUE : BooleanNode.FALSE);
//                JsonUtils.saveToFile(parameterOverrides, parameterOverridesFile);
//
//                // Run analysis
//                ProgressInfo jipipeProgress = progressInfo.resolveAndLog("Running JIPipe");
//                CommandLine commandLine;
//
//                if(StringUtils.isNullOrEmpty(runtimeConfig.getFijiWrapper()) || !runtimeConfig.isFijiWrapperEnabled()) {
//                    Path jipipeExecutablePath = Path.of(runtimeConfig.getFijiExecutablePath());
//                    commandLine = new CommandLine(jipipeExecutablePath.toFile());
//                }
//                else {
//                    Path jipipeExecutablePath = Path.of(runtimeConfig.getFijiExecutablePath());
//                    commandLine = new CommandLine(Path.of(runtimeConfig.getFijiWrapper()).toFile());
//
//                    for (String arg : runtimeConfig.getFijiWrapperArgs()) {
//                        commandLine.addArgument(arg);
//                    }
//
//                    commandLine.addArgument(jipipeExecutablePath.toAbsolutePath().toString());
//                }
//
//                Path jipipeRootPath = Path.of(runtimeConfig.getFijiPath());
//                progressInfo.incrementProgress();
//
//                for (String arg : runtimeConfig.getFijiArgs()) {
//                    commandLine.addArgument(arg);
//                }
//
//
//                commandLine.addArgument("--pass-classpath");
//                commandLine.addArgument("--full-classpath");
//                commandLine.addArgument("--main-class");
//                commandLine.addArgument("org.hkijena.jipipe.JIPipeCLI");
//                commandLine.addArgument("run");
//                commandLine.addArgument("--project");
//                commandLine.addArgument(projectFile.toAbsolutePath().toString());
//                commandLine.addArgument("--output-folder");
//                commandLine.addArgument(jipipeOutputDirectory.toAbsolutePath().toString());
//                commandLine.addArgument("--output-results");
//                commandLine.addArgument("only-compartment-outputs");
//                commandLine.addArgument("--overwrite-parameters");
//                commandLine.addArgument(parameterOverridesFile.toAbsolutePath().toString());
//
//                ProcessUtils.ExtendedExecutor executor = new ProcessUtils.ExtendedExecutor(ExecuteWatchdog.INFINITE_TIMEOUT, jipipeProgress, lockFilePath);
//                executor.setWorkingDirectory(jipipeRootPath.toFile());
//                ProcessUtils.setupLogger(commandLine, executor, jipipeProgress);
//
//                try {
//                    executor.execute(commandLine);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//
//                if(!Files.isRegularFile(lockFilePath)) {
//                    throw new InterruptedException();
//                }
//
//                // Postprocessing results
//                progressInfo.log("Postprocessing results");
//                progressInfo.incrementProgress();
//                timeSeries.clearOutputData();
//
//                Path resultsAllInOneFile = workDirectory.resolve("results").resolve("results_all_in_one.csv");
//                CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
//                try(FileReader reader = new FileReader(resultsAllInOneFile.toFile())) {
//                    for (CSVRecord record : csvFormat.parse(reader)) {
//                        OutputData outputData = new OutputData();
//
//                        String experiment = record.get("#Experiment");
//                        String sample = record.get("#Sample");
//                        double fog = NumberUtils.createDouble(record.get("FoG"));
//                        double rad = NumberUtils.createDouble(record.get("RAD_mm"));
//                        double threshold = NumberUtils.createDouble(record.get("#Threshold"));
//                        List<Image> inputData = imageRepository.findByDatasetAndExperimentAndSample(timeSeries, experiment, sample);
//
//                        outputData.setExperiment(experiment);
//                        outputData.setSample(sample);
//                        outputData.setFog(fog);
//                        outputData.setRad(rad);
//                        outputData.setThreshold(threshold * 100);
//                        outputData.setInputData(inputData);
//                        outputData.setAssayType(inputData.get(0).getAssayType());
//
//                        Path visualizationPath = workDirectory.resolve("results").resolve("visualizations").resolve("ZOI").resolve(experiment + "_" + sample + "_t" + threshold + ".png");
//                        Path visualizationThumbnailPath = Files.createTempFile(resultServiceDirectory, "thumbnail", ".png");
//                        ImageUtils.createThumbnail(ImageIO.read(visualizationPath.toFile()), 128, 64, visualizationThumbnailPath);
//                        outputData.setVisualizationStoragePath(visualizationPath.toAbsolutePath().toString());
//                        outputData.setVisualizationThumbnailStoragePath(visualizationThumbnailPath.toAbsolutePath().toString());
//
//                        outputData = outputDataRepository.save(outputData);
//                        timeSeries.addOutputData(outputData);
//                    }
//                }
//
//                if(!Files.isRegularFile(lockFilePath)) {
//                    throw new InterruptedException();
//                }
//
//                // ZIP analysis results
//                Path zipFile = resultServiceDirectory.resolve("results.zip");
//                ProgressInfo zipProgress = progressInfo.resolveAndLog("Compressing results");
//                progressInfo.incrementProgress();
//                ArchiveUtils.zipDirectory(workDirectory, StringUtils.makeFilesystemCompatible("" + timeSeries.getName()), zipFile, zipProgress);
//
//                if(!Files.isRegularFile(lockFilePath)) {
//                    throw new InterruptedException();
//                }
//
//                // Finalize the analysis
//                timeSeries.setStatus(TimeSeries.Status.RunFinished);
//                timeSeriesRepository.save(timeSeries);
//            }
//            catch (Throwable e) {
//                timeSeries.setStatus(TimeSeries.Status.RunInterrupted);
//                timeSeriesRepository.save(timeSeries);
//                e.printStackTrace();
//                logError(e.toString(), context, timeSeries);
//            }
//            finally {
//                try {
//                    Files.deleteIfExists(lockFilePath);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
}
