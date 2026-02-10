package org.hkijena.jast.controller;

import org.hkijena.jast.config.PresetsConfig;
import org.hkijena.jast.payloads.StripPresetPayload;
import org.hkijena.jast.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class PresetsController {
    private final UserService userService;
    private final PresetsConfig presetsConfig;

    @Autowired
    public PresetsController(UserService userService, PresetsConfig presetsConfig) {
        this.userService = userService;
        this.presetsConfig = presetsConfig;
    }

    @GetMapping("/api/get-presets/strip")
    public ResponseEntity<List<StripPresetPayload>> stripPresets(Authentication authentication) {
        userService.validateAuthentication(authentication);
        List<StripPresetPayload> result = new ArrayList<>();
        List<StripPresetPayload> globalStripPresets = presetsConfig.getStripPresets();

        // Assign fake IDs to global presets to allow Vue key mechanism
        for (int i = 0; i < globalStripPresets.size(); i++) {
            StripPresetPayload copy = new StripPresetPayload(globalStripPresets.get(i));
            copy.setId(-(i + 1));
            result.add(copy);
        }

        // TODO: user presets

        return ResponseEntity.ok(result);
    }
}
