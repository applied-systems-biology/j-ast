package org.hkijena.jast.controller;

import org.hkijena.jast.config.RuntimeConfig;
import org.hkijena.jast.model.entities.BackendTask;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.payloads.task.BackendTaskPayload;
import org.hkijena.jast.payloads.task.BackendTaskTypePayload;
import org.hkijena.jast.repositories.BackendTaskRepository;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.services.BackendTaskRegistry;
import org.hkijena.jast.services.BackendTaskService;
import org.hkijena.jast.services.ProjectService;
import org.hkijena.jast.tasks.BackendTaskWorkload;
import org.hkijena.jast.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
public class TaskController {
    private final ProjectService projectService;
    private final ImageRepository imageRepository;
    private final BackendTaskRegistry backendTaskRegistry;
    private final ProjectRepository projectRepository;
    private final BackendTaskRepository backendTaskRepository;
    private final BackendTaskService backendTaskService;
    private final RuntimeConfig runtimeConfig;

    @Autowired
    public TaskController(ProjectService projectService, ImageRepository imageRepository, BackendTaskRegistry backendTaskRegistry, ProjectRepository projectRepository, BackendTaskRepository backendTaskRepository, BackendTaskService backendTaskService, RuntimeConfig runtimeConfig) {
        this.projectService = projectService;
        this.imageRepository = imageRepository;
        this.backendTaskRegistry = backendTaskRegistry;
        this.projectRepository = projectRepository;
        this.backendTaskRepository = backendTaskRepository;
        this.backendTaskService = backendTaskService;
        this.runtimeConfig = runtimeConfig;
    }

    @GetMapping("/api/task/list-types")
    public ResponseEntity<List<BackendTaskTypePayload>> listAvailableTasks() {
        return ResponseEntity.ok(backendTaskRegistry.getRegisteredTasks().entrySet().stream().map(entry -> {
            BackendTaskWorkload workload = entry.getValue();
            return new BackendTaskTypePayload(workload, entry.getKey());
        }).toList());
    }

    @GetMapping("/api/project/{id}/tasks")
    public ResponseEntity<List<BackendTaskPayload>> listTasksForProject(@PathVariable long id, Authentication authentication) {
        Project project = projectService.getProjectByIdOrError(id);
        if (!project.canAccess(authentication)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(project.getTasks().stream().map(BackendTaskPayload::new).toList());
    }


    @PostMapping("/api/project/{id}/clear-tasks")
    public void clearProjectTasks(@PathVariable long id, Authentication authentication) {
        Project project = projectService.getProjectByIdOrError(id);
        if (!project.canEdit(authentication)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        project.clearTasks();
        projectRepository.save(project);
    }

    @PostMapping("/api/task/new")
    public ResponseEntity<BackendTaskPayload> startNewTask(@RequestBody BackendTaskPayload payload, Authentication authentication) {
        Project project = projectService.getProjectByIdOrError(payload.getProjectId());
        if (!project.canEdit(authentication)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        // Check if the task is valid
        BackendTaskWorkload taskWorkload = backendTaskRegistry.getTask(payload.getTaskId());
        if (taskWorkload == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        // Check the images
//        List<Image> images = new ArrayList<>();
        for (long imageId : payload.getImageIds()) {
            Optional<Image> image_ = imageRepository.findById(imageId);
            if (image_.isPresent()) {
                // Check if the project is actually the same
                if (!Objects.equals(image_.get().getProject().getId(), project.getId())) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Image " + imageId + " is not part of project " + project.getId());
                }

//                images.add(image_.get());
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No image found with id " + imageId);
            }
        }

        // We queue the payload into the system and let it be picked up by the task service automatically
        BackendTask task = new BackendTask();
        task.setName(taskWorkload.getName());
        task.setPayload(JsonUtils.toJsonString(payload));
        task.setProject(project);
        task.setTaskTypeId(payload.getTaskId());
        task.setTmpPath(backendTaskService.createTmpPath().toAbsolutePath().normalize().toString());
        task = backendTaskRepository.save(task);

        project.addTask(task);
        projectRepository.save(project);

        // Update the payload
        payload.setId(task.getId());

        // Immediately schedule the job
        backendTaskService.enqueueTask(task);

        return ResponseEntity.ok(payload);
    }

    @GetMapping("/api/task/{id}/running-log")
    public ResponseEntity<String> getRunningTaskLog(@PathVariable long id, Authentication authentication) {
        Optional<BackendTask> task_ = backendTaskRepository.findById(id);
        if (task_.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        BackendTask task = task_.get();
        if (!task.getProject().canAccess(authentication)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (task.isRunning()) {

        }
        return ResponseEntity.ok("Status: " + task.getStatus());
    }

    @GetMapping("/api/task/{id}/log")
    public ResponseEntity<String> getTaskLog(@PathVariable long id, Authentication authentication) {
        Optional<BackendTask> task_ = backendTaskRepository.findById(id);
        if (task_.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        BackendTask task = task_.get();
        if (!task.getProject().canAccess(authentication)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(task.getLog());
    }
}
