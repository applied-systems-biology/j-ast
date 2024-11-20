package org.hkijena.jast.controller;

import org.hkijena.jast.payloads.task.BackendTaskInfoPayload;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.services.BackendTaskRegistry;
import org.hkijena.jast.services.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TaskController {
    private final ProjectService projectService;
    private final ImageRepository imageRepository;
    private final BackendTaskRegistry backendTaskRegistry;

    @Autowired
    public TaskController(ProjectService projectService, ImageRepository imageRepository, BackendTaskRegistry backendTaskRegistry) {
        this.projectService = projectService;
        this.imageRepository = imageRepository;
        this.backendTaskRegistry = backendTaskRegistry;
    }

    @GetMapping("/api/task/list-types")
    public ResponseEntity<List<BackendTaskInfoPayload>> listAvailableTasks() {
        return ResponseEntity.ok(backendTaskRegistry.getRegisteredTasks().entrySet().stream().map(entry -> {
            BackendTaskInfoPayload payload = new BackendTaskInfoPayload();
            payload.setTaskId(entry.getKey());
            payload.setName(entry.getValue().getName());
            payload.setDescription(entry.getValue().getDescription());
            return payload;
        }).toList());
    }
}
