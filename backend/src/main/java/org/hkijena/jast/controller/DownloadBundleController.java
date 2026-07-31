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
import org.hkijena.jast.model.entities.DownloadBundle;
import org.hkijena.jast.payloads.downloadbundle.DownloadBundlePayload;
import org.hkijena.jast.services.DownloadBundleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class DownloadBundleController {

    private final DownloadBundleService downloadBundleService;

    @Autowired
    public DownloadBundleController(DownloadBundleService downloadBundleService) {
        this.downloadBundleService = downloadBundleService;
    }

    @PostMapping("/api/result/{id}/prepare-download")
    public ResponseEntity<DownloadBundlePayload> prepareDownload(
            Authentication authentication,
            @PathVariable("id") long id,
            @RequestParam(value = "path", required = false, defaultValue = "/") String path
    ) {
        DownloadBundle bundle = downloadBundleService.prepareDownload(id, path, authentication);
        return ResponseEntity.ok(new DownloadBundlePayload(bundle));
    }

    @GetMapping("/api/download-bundle/{bundleId}")
    public ResponseEntity<DownloadBundlePayload> getBundle(
            Authentication authentication,
            @PathVariable String bundleId
    ) {
        DownloadBundle bundle = downloadBundleService.getBundle(bundleId, authentication);
        return ResponseEntity.ok(new DownloadBundlePayload(bundle));
    }

    @GetMapping("/api/download-bundle/{bundleId}/part/{partIndex}")
    public void downloadPart(
            HttpServletResponse response,
            Authentication authentication,
            @PathVariable String bundleId,
            @PathVariable int partIndex
    ) throws IOException {
        Path filePath = downloadBundleService.getPartFilePath(bundleId, partIndex, authentication);
        String fileName = downloadBundleService.getPartFileName(bundleId, partIndex, authentication);

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Length", Long.toString(Files.size(filePath)));

        OutputStream outputStream = response.getOutputStream();
        try (var inputStream = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }
        }
        outputStream.flush();
    }
}
