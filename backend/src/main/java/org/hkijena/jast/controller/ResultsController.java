package org.hkijena.jast.controller;

import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.model.entities.Result;
import org.hkijena.jast.payloads.result.FullResultPayload;
import org.hkijena.jast.payloads.result.ResultPayload;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.repositories.ResultRepository;
import org.hkijena.jast.services.ProjectService;
import org.hkijena.jast.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class ResultsController {

    private final ProjectService projectService;
    private final UserService userService;
    private final ProjectRepository projectRepository;
    private final ResultRepository resultRepository;

    @Autowired
    public ResultsController(ProjectService projectService, UserService userService, ProjectRepository projectRepository, ResultRepository resultRepository) {
        this.projectService = projectService;
        this.userService = userService;
        this.projectRepository = projectRepository;
        this.resultRepository = resultRepository;
    }

    @GetMapping("/api/project/{id}/list-results")
    public ResponseEntity<List<ResultPayload>> listResultsForProject(@PathVariable("id") long projectId, Authentication authentication) {
        userService.validateAuthentication(authentication);
        Project project = projectService.getProjectByIdOrError(projectId);
        if (!project.canAccess(authentication)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        List<ResultPayload> result = new ArrayList<>();
        for (Result projectResult : project.getResults()) {
            result.add(new ResultPayload(projectResult));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/result/{id}")
    public ResponseEntity<FullResultPayload> getFullResult(@PathVariable("id") long id, Authentication authentication) {
        userService.validateAuthentication(authentication);
        Optional<Result> result_ = resultRepository.findById(id);
        if(result_.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Result result = result_.get();
        if(!result.getProject().canAccess(authentication)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if(!result.isViewed()) {
            result.setViewed(true);
            resultRepository.save(result);
        }
        return ResponseEntity.ok(new FullResultPayload(result));
    }

    @PostMapping("/api/result/{id}/delete")
    public void deleteResult(@PathVariable("id") long id, Authentication authentication) {
        userService.validateAuthentication(authentication);
        Optional<Result> result_ = resultRepository.findById(id);
        if(result_.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Result result = result_.get();
        if(!result.getProject().canEdit(authentication)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        resultRepository.deleteById(id);
    }

    @PostMapping("/api/result/{id}/mark-viewed")
    public void markResultViewed(@PathVariable("id") long id, Authentication authentication) {
        userService.validateAuthentication(authentication);
        Optional<Result> result_ = resultRepository.findById(id);
        if(result_.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Result result = result_.get();
        if(!result.getProject().canEdit(authentication)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        result.setViewed(true);
        resultRepository.save(result);
    }

}
