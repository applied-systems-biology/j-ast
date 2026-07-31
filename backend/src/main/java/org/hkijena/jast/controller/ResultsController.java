/*
 * Copyright (c) 2026.
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jast.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.hkijena.jast.config.AccountConfig;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    @GetMapping("/api/result/{id}/download-zip")
    public void downloadResultZip(
            HttpServletResponse response,
            Authentication authentication,
            @PathVariable("id") long id,
            @RequestParam(value = "path", required = false, defaultValue = "/") String path
    ) throws IOException {
        userService.validateAuthentication(authentication);
        Optional<Result> result_ = resultRepository.findById(id);
        if (result_.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Result result = result_.get();
        if (!result.getProject().canAccess(authentication, accountConfig)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        String normalizedPath = path == null ? "" : path;
        while (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }

        String zipFileName = sanitizeFileName(result.getName()) + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFileName + "\"");

        OutputStream outputStream = response.getOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
            byte[] buffer = new byte[8192];
            for (ResultItem item : result.getResultItems()) {
                String itemPath = item.getPath() == null ? "" : item.getPath();
                String itemName = item.getName() == null ? "unnamed" : item.getName();

                String displayPath = "/" + itemPath;
                String filterPath = path == null ? "/" : (path.startsWith("/") ? path : "/" + path);
                if (!displayPath.startsWith(filterPath)) {
                    continue;
                }

                String entryName = itemPath + "/" + itemName;
                if (!normalizedPath.isEmpty() && entryName.startsWith(normalizedPath)) {
                    entryName = entryName.substring(normalizedPath.length());
                }
                while (entryName.startsWith("/")) {
                    entryName = entryName.substring(1);
                }
                if (entryName.isEmpty()) {
                    entryName = itemName;
                }

                String rawDataFileId = item.getRawDataFileId();
                if (rawDataFileId == null || rawDataFileId.isEmpty()) {
                    continue;
                }
                Path filePath = fileStorageService.getFilePath(rawDataFileId);
                if (filePath == null || !Files.exists(filePath)) {
                    continue;
                }

                ZipEntry zipEntry = new ZipEntry(entryName);
                zipOut.putNextEntry(zipEntry);
                try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zipOut.write(buffer, 0, len);
                    }
                }
                zipOut.closeEntry();
            }
        }
    }

    private String sanitizeFileName(String input) {
        if (input == null || input.isEmpty()) return "result";
        String clean = input.replaceAll("[<>:\"/\\\\|?*\\x00]", "_").trim();
        if (clean.isEmpty()) return "result";
        if (clean.length() > 255) {
            clean = clean.substring(0, 255);
        }
        return clean;
    }

}
