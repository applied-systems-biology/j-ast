package org.hkijena.jipipe.webapp.growthassay.controller;

import org.hkijena.jipipe.webapp.growthassay.model.UserPrincipal;
import org.hkijena.jipipe.webapp.growthassay.model.ZOIShapePreset;
import org.hkijena.jipipe.webapp.growthassay.repositories.ZOIShapePresetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ZOIShapePresetController {
    private final ZOIShapePresetRepository zoiShapePresetRepository;

    public ZOIShapePresetController(ZOIShapePresetRepository zoiShapePresetRepository) {
        this.zoiShapePresetRepository = zoiShapePresetRepository;
    }

    @GetMapping("zoi-shapes")
    public ModelAndView overview(Model model, Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        model.addAttribute("presets", zoiShapePresetRepository.getByAuthentication(authentication));
        return new ModelAndView("zoi-shape-preset-editor");
    }

    @GetMapping("zoi-shapes/add")
    public ModelAndView add(Model model, Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // Add new item
        ZOIShapePreset preset = new ZOIShapePreset();
        if(authentication.getPrincipal() instanceof UserPrincipal) {
            preset.setOwner(((UserPrincipal) authentication.getPrincipal()).getUser());
        }
        zoiShapePresetRepository.save(preset);

        return new ModelAndView("redirect:/zoi-shapes");
    }
}
