package org.hkijena.jast.controller;

import org.hkijena.jast.config.ProviderConfig;
import org.hkijena.jast.payloads.ProviderInfoPayload;
import org.hkijena.jast.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
public class ProviderController {

    private final ProviderConfig providerConfig;

    @Autowired
    public ProviderController(ProviderConfig providerConfig) {
        this.providerConfig = providerConfig;
    }

    @GetMapping("/api/provider/info")
    public ResponseEntity<ProviderInfoPayload> getProviderInfo() {
        ProviderInfoPayload payload = new ProviderInfoPayload();
        payload.setProviderName(providerConfig.getProviderName());
        payload.setProviderUrl(providerConfig.getProviderUrl());
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/api/provider/imprint")
    public ResponseEntity<String> getImprint() {
        if(!StringUtils.isNullOrEmpty(providerConfig.getImprintFile()) && Files.isRegularFile(Paths.get(providerConfig.getImprintFile()))) {
            try {
                String s = Files.readString(Paths.get(providerConfig.getImprintFile()));
                return ResponseEntity.ok(s);
            } catch (Exception ignored) {
            }
        }
        return ResponseEntity.ok("No imprint found!");
    }

    @GetMapping("/api/provider/privacy")
    public ResponseEntity<String> getPrivacyStatement() {
        if(!StringUtils.isNullOrEmpty(providerConfig.getPrivacyStatementFile()) && Files.isRegularFile(Paths.get(providerConfig.getPrivacyStatementFile()))) {
            try {
                String s = Files.readString(Paths.get(providerConfig.getPrivacyStatementFile()));
                return ResponseEntity.ok(s);
            } catch (Exception ignored) {
            }
        }
        return ResponseEntity.ok("No privacy statement found!");
    }
}
