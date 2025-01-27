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
import org.jobrunr.jobs.JobId;
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
    private final FileStorageService fileStorageService;

    @Autowired
    public BackendTaskService(RuntimeConfig runtimeConfig, ImageRepository imageRepository, ProjectRepository projectRepository, BackendTaskRegistry backendTaskRegistry, BackendTaskRepository backendTaskRepository, JobScheduler jobScheduler, FileStorageService fileStorageService) {
        this.runtimeConfig = runtimeConfig;
        this.projectRepository = projectRepository;
        this.imageRepository = imageRepository;
        this.backendTaskRegistry = backendTaskRegistry;
        this.backendTaskRepository = backendTaskRepository;
        this.jobScheduler = jobScheduler;
        this.fileStorageService = fileStorageService;
    }

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

            JobId jobId = jobScheduler.create(JobBuilder.aJob().withLabels("taskId-" + newTask.getId(), "projectId-" + newTask.getProject().getId()).withDetails(() -> startBackendTask(params, JobContext.Null)));
            newTask.setJobId(jobId.asUUID().toString());
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
                task.setLog(fileStorageService, progressInfo.getLog().toString());
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
                task.setLog(fileStorageService, progressInfo.getLog().toString());
                backendTaskRepository.save(task);
            }
        }
        finally {
            if(!runtimeConfig.isKeepTmp()) {
                logger.info("Deleting temporary directory " + params.getTmpPath());
                try {
                    FileSystemUtils.deleteRecursively( params.getTmpPath().toFile());
                } catch (Exception e) {
                    logger.error("Failed to delete temporary directory {}",  params.getTmpPath(), e);
                }
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
}
