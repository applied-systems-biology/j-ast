package org.hkijena.jast.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.config.WebSecurityConfig;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.model.entities.Result;
import org.hkijena.jast.model.entities.ResultItem;
import org.hkijena.jast.payloads.result.FullResultPayload;
import org.hkijena.jast.payloads.result.ResultPayload;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.repositories.ResultItemRepository;
import org.hkijena.jast.repositories.ResultRepository;
import org.hkijena.jast.services.FileStorageService;
import org.hkijena.jast.services.ProjectService;
import org.hkijena.jast.services.UserService;
import org.hkijena.jast.utils.MimeTypeUtils;
import org.hkijena.jast.utils.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class ResultsController {

    private final ProjectService projectService;
    private final UserService userService;
    private final ProjectRepository projectRepository;
    private final ResultRepository resultRepository;
    private final ResultItemRepository resultItemRepository;
    private final FileStorageService fileStorageService;
    private final AccountConfig accountConfig;

    @Autowired
    public ResultsController(ProjectService projectService, UserService userService, ProjectRepository projectRepository, ResultRepository resultRepository, ResultItemRepository resultItemRepository, FileStorageService fileStorageService, AccountConfig accountConfig) {
        this.projectService = projectService;
        this.userService = userService;
        this.projectRepository = projectRepository;
        this.resultRepository = resultRepository;
        this.resultItemRepository = resultItemRepository;
        this.fileStorageService = fileStorageService;
        this.accountConfig = accountConfig;
    }

    @GetMapping("/api/project/{id}/list-results")
    public ResponseEntity<List<ResultPayload>> listResultsForProject(@PathVariable("id") long projectId, Authentication authentication) {
        userService.validateAuthentication(authentication);
        Project project = projectService.getProjectByIdOrError(projectId);
        if (!project.canAccess(authentication, accountConfig)) {
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
        if (result_.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Result result = result_.get();
        if (!result.getProject().canAccess(authentication, accountConfig)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (!result.isViewed()) {
            result.setViewed(true);
            resultRepository.save(result);
        }
        return ResponseEntity.ok(new FullResultPayload(result));
    }

    @GetMapping("/api/result-item/{id}/thumbnail")
    public void getThumbnail(HttpServletResponse response, Authentication authentication, @PathVariable long id) throws IOException {
        userService.validateAuthentication(authentication);
        Optional<ResultItem> resultItem_ = resultItemRepository.findById(id);
        if (resultItem_.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        ResultItem resultItem = resultItem_.get();
        RequestUtils.sendContent(response, resultItem.getThumbnailData(fileStorageService), MimeTypeUtils.MIME_TYPE_PNG);
    }

    @GetMapping("/api/result-item/{id}/raw")
    public void getRaw(HttpServletResponse response, Authentication authentication, @PathVariable long id) throws IOException {
        userService.validateAuthentication(authentication);
        Optional<ResultItem> resultItem_ = resultItemRepository.findById(id);
        if (resultItem_.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        ResultItem resultItem = resultItem_.get();
        RequestUtils.sendContent(response, resultItem.getRawData(fileStorageService), MimeTypeUtils.MIME_TYPE_OCTET_STREAM);
    }

    @PostMapping("/api/result/{id}/delete")
    public void deleteResult(@PathVariable("id") long id, Authentication authentication) {
        userService.validateAuthentication(authentication);
        Optional<Result> result_ = resultRepository.findById(id);
        if (result_.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Result result = result_.get();
        if (!result.getProject().canEdit(authentication, accountConfig)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        result.deleteFilesLater(fileStorageService);
        resultRepository.deleteById(id);
    }

    @PostMapping("/api/result/{id}/mark-viewed")
    public void markResultViewed(@PathVariable("id") long id, Authentication authentication) {
        userService.validateAuthentication(authentication);
        Optional<Result> result_ = resultRepository.findById(id);
        if (result_.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Result result = result_.get();
        if (!result.getProject().canEdit(authentication, accountConfig)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        result.setViewed(true);
        resultRepository.save(result);
    }

}
