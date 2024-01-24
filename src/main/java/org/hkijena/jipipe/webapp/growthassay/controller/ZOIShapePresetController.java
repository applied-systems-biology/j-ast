package org.hkijena.jipipe.webapp.growthassay.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.hkijena.jipipe.webapp.growthassay.model.*;
import org.hkijena.jipipe.webapp.growthassay.model.messages.UpdateZOIShapePresetMessage;
import org.hkijena.jipipe.webapp.growthassay.repositories.DatasetRepository;
import org.hkijena.jipipe.webapp.growthassay.repositories.ZOIShapePresetRepository;
import org.hkijena.jipipe.webapp.growthassay.utils.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Controller
public class ZOIShapePresetController {
    private final DatasetRepository datasetRepository;
    private final ZOIShapePresetRepository zoiShapePresetRepository;

    public ZOIShapePresetController(DatasetRepository datasetRepository, ZOIShapePresetRepository zoiShapePresetRepository) {
        this.datasetRepository = datasetRepository;
        this.zoiShapePresetRepository = zoiShapePresetRepository;
    }

    @GetMapping("zoi-shapes")
    public ModelAndView overview(Model model, Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        datasetRepository.putSortedToModel(model, authentication);
        model.addAttribute("editablePresets", zoiShapePresetRepository.getEditableByAuthentication(authentication));
        model.addAttribute("readonlyPresets", zoiShapePresetRepository.getReadonlyByAuthentication(authentication));
        return new ModelAndView("zoi-shape-preset-editor");
    }

    @GetMapping("zoi-shapes/add")
    public ModelAndView add(Authentication authentication) {
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

    @GetMapping("zoi-shapes/delete/{items}")
    public ModelAndView delete(Authentication authentication, RedirectAttributes redirectAttributes, @PathVariable String items) {
        if(authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if(StringUtils.isNullOrEmpty(items)) {
            return new ModelAndView("redirect:/zoi-shapes");
        }
        List<Long> toRemove = new ArrayList<>();
        for (String s : items.split(",")) {
            long id = Long.parseLong(s);
            Optional<ZOIShapePreset> byId = zoiShapePresetRepository.findById(id);
            if(byId.isPresent()) {
                boolean allowed = authentication.getPrincipal() instanceof AdminPrincipal;
                if(authentication.getPrincipal() instanceof  UserPrincipal) {
                    User user = ((UserPrincipal) authentication.getPrincipal()).getUser();
                    allowed = user.getRole() == User.Role.Admin || Objects.equals(user.getId(), byId.get().getOwner().getId());
                }
                if (allowed) {
                    toRemove.add(id);
                }
            }
        }

        // Remove
        zoiShapePresetRepository.deleteAllById(toRemove);

        Notification.pushToRedirect("ZOI shapes deleted", toRemove.size() + " ZOI shapes were deleted", Notification.Style.success, redirectAttributes);
        return new ModelAndView("redirect:/zoi-shapes");
    }

    @PostMapping("/zoi-shapes/update")
    public void update(HttpServletResponse response, Authentication authentication, @RequestBody UpdateZOIShapePresetMessage message) {
        for (UpdateZOIShapePresetMessage.ZOIShapePresetMessage presetMessage : message.getPresets().values()) {
            Optional<ZOIShapePreset> byId = zoiShapePresetRepository.findById(presetMessage.getId());
            if(byId.isPresent()) {
                boolean isAdmin = authentication.getPrincipal() instanceof AdminPrincipal;
                boolean allowed = authentication.getPrincipal() instanceof AdminPrincipal;
                ZOIShapePreset preset = byId.get();
                if(authentication.getPrincipal() instanceof  UserPrincipal) {
                    User user = ((UserPrincipal) authentication.getPrincipal()).getUser();
                    allowed = user.getRole() == User.Role.Admin || Objects.equals(user.getId(), preset.getOwner().getId());
                    isAdmin = user.getRole() == User.Role.Admin;
                }
                if(allowed) {
                    preset.setName(presetMessage.getName());
                    preset.setDescription(presetMessage.getDescription());
                    preset.setMcaRate(presetMessage.getMcaRate());
                    preset.setMcaIntercept(presetMessage.getMcaIntercept());
                    preset.setStripRate(presetMessage.getStripRate());
                    preset.setStripIntercept(presetMessage.getStripIntercept());
                    if(isAdmin) {
                        preset.setGlobal(presetMessage.isGlobal());
                    }
                    zoiShapePresetRepository.save(preset);
                }
            }
        }

    }
}
