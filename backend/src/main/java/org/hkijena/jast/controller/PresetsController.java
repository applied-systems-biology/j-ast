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

import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.config.PresetsConfig;
import org.hkijena.jast.model.entities.Preset;
import org.hkijena.jast.payloads.StripPresetInterpolation;
import org.hkijena.jast.payloads.StripPresetPayload;
import org.hkijena.jast.repositories.PresetRepository;
import org.hkijena.jast.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Controller
public class PresetsController {
    private final UserService userService;
    private final PresetsConfig presetsConfig;
    private final PresetRepository presetRepository;
    private final AccountConfig accountConfig;

    @Autowired
    public PresetsController(UserService userService, PresetsConfig presetsConfig, PresetRepository presetRepository, AccountConfig accountConfig) {
        this.userService = userService;
        this.presetsConfig = presetsConfig;
        this.presetRepository = presetRepository;
        this.accountConfig = accountConfig;
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

        // Add user presets
        for (Preset preset : presetRepository.getByAuthentication(authentication, accountConfig)) {
            if ("strip".equals(preset.getType())) {
                StripPresetPayload payload = new StripPresetPayload();
                payload.setId(preset.getId());
                payload.setName(preset.getName());

                // Parse interpolation
                try {
                    Object interpolation = preset.getData().getOrDefault("interpolation", "Linear");
                    payload.setInterpolation(StripPresetInterpolation.valueOf(interpolation.toString()));
                } catch (Throwable ignored) {
                }

                // Parse ticks
                try {
                    Object ticks = preset.getData().getOrDefault("ticks", Collections.emptyList());
                    if (!(ticks instanceof Collection)) {
                        ticks = Collections.emptyList();
                    }
                    for (Object tick : (Collection) ticks) {
                        if (tick instanceof Number n) {
                            payload.getTicks().add(n.doubleValue());
                        }
                    }
                } catch (Throwable ignored) {
                }

                result.add(payload);

            }
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/add-preset/strip")
    public ResponseEntity<String> addStripPreset(Authentication authentication, @RequestBody StripPresetPayload preset) {
        userService.validateAuthentication(authentication);

        Preset entity = new Preset();
        entity.setOwner(userService.authenticationToUser(authentication));
        entity.setType("strip");
        entity.setName(preset.getName());
        entity.setData(new HashMap<>());
        entity.getData().put("ticks", preset.getTicks());
        entity.getData().put("interpolation", preset.getInterpolation());
        presetRepository.save(entity);

        return ResponseEntity.ok("Successfully added preset");
    }

    @PostMapping("/api/update-preset/strip")
    public ResponseEntity<String> updateStripPreset(Authentication authentication, @RequestBody StripPresetPayload preset) {
        userService.validateAuthentication(authentication);
        Optional<Preset> byId = presetRepository.findById(preset.getId());
        if (byId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Preset entity = byId.get();
        if (!entity.isOwnedBy(authentication, accountConfig)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        entity.setName(preset.getName());
        entity.getData().put("ticks", preset.getTicks());
        entity.getData().put("interpolation", preset.getInterpolation());
        presetRepository.save(entity);

        return ResponseEntity.ok("Successfully edited preset");
    }

    @PostMapping("/api/delete-preset/{id}")
    public ResponseEntity<String> deletePreset(Authentication authentication, @PathVariable long id) {
        userService.validateAuthentication(authentication);
        Optional<Preset> byId = presetRepository.findById(id);
        if (byId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Preset entity = byId.get();
        if (!entity.isOwnedBy(authentication, accountConfig)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        presetRepository.delete(entity);

        return ResponseEntity.ok("Successfully deleted preset");
    }
}
