package org.hkijena.jast.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpServletResponse;
import org.hkijena.jast.model.*;
import org.hkijena.jipipe.webapp.growthassay.model.*;
import org.hkijena.jast.model.messages.UpdateZOIShapePresetMessage;
import org.hkijena.jast.repositories.DatasetRepository;
import org.hkijena.jast.repositories.ZOIShapePresetRepository;
import org.hkijena.jast.utils.JsonUtils;
import org.hkijena.jast.utils.RequestUtils;
import org.hkijena.jast.utils.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @PostMapping("zoi-shapes/upload")
    public ModelAndView upload(Authentication authentication, @RequestParam("file") MultipartFile file,
                               RedirectAttributes redirectAttributes) {
        if(authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        boolean isAdmin = authentication.getPrincipal() instanceof AdminPrincipal;
        if(authentication.getPrincipal() instanceof  UserPrincipal) {
            User user = ((UserPrincipal) authentication.getPrincipal()).getUser();
            isAdmin = user.getRole() == User.Role.Admin;
        }
        User owner = authentication.getPrincipal() instanceof UserPrincipal ? ((UserPrincipal) authentication.getPrincipal()).getUser() : null;

        try (InputStream stream = file.getInputStream()) {
            TypeReference<List<UpdateZOIShapePresetMessage.ZOIShapePresetMessage>> typeReference = new TypeReference<>() { };
            List<UpdateZOIShapePresetMessage.ZOIShapePresetMessage> items = JsonUtils.getObjectMapper().readerFor(typeReference).readValue(stream);
            for (UpdateZOIShapePresetMessage.ZOIShapePresetMessage presetMessage : items) {
                ZOIShapePreset preset = new ZOIShapePreset();
                preset.setOwner(owner);
                preset.setName(presetMessage.getName());
                preset.setDescription(presetMessage.getDescription());
                preset.setMcaRate(presetMessage.getMcaRate());
                preset.setMcaIntercept(presetMessage.getMcaIntercept());
                preset.setStripRate(presetMessage.getStripRate());
                preset.setStripIntercept(presetMessage.getStripIntercept());
                preset.setGlobal(presetMessage.isGlobal());
                if(!isAdmin) {
                  preset.setGlobal(false);
                }
                zoiShapePresetRepository.save(preset);
            }

            Notification.pushToRedirect("Upload successful", "The ZOI shapes were successfully imported", Notification.Style.success, redirectAttributes);
            return new ModelAndView("redirect:/zoi-shapes");
        }
        catch (Throwable e) {
            Notification.pushToRedirect("Error during upload", "The import failed with the message '" + e.getMessage() + "'", Notification.Style.danger, redirectAttributes);
            return new ModelAndView("redirect:/zoi-shapes");
        }
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

    @GetMapping("zoi-shapes/download/{items}")
    public ModelAndView download(HttpServletResponse response, Authentication authentication, @PathVariable String items) {
        if(authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        List<UpdateZOIShapePresetMessage.ZOIShapePresetMessage> instances = new ArrayList<>();
        for (String s : items.split(",")) {
            long id = Long.parseLong(s);
            Optional<ZOIShapePreset> byId = zoiShapePresetRepository.findById(id);
            if(byId.isPresent()) {
                ZOIShapePreset preset = byId.get();
                if(preset.isGlobal()
                        || authentication.getPrincipal() instanceof AdminPrincipal
                        || (authentication.getPrincipal() instanceof UserPrincipal && preset.getOwner() != null && Objects.equals(((UserPrincipal) authentication.getPrincipal()).getUser().getId(), preset.getOwner().getId()))) {
                    UpdateZOIShapePresetMessage.ZOIShapePresetMessage message = new UpdateZOIShapePresetMessage.ZOIShapePresetMessage();
                    message.setId(preset.getId());
                    message.setName(preset.getName());
                    message.setDescription(preset.getDescription());
                    message.setGlobal(preset.isGlobal());
                    message.setMcaRate(preset.getMcaRate());
                    message.setMcaIntercept(preset.getMcaIntercept());
                    message.setStripIntercept(preset.getStripIntercept());
                    message.setStripRate(preset.getStripRate());
                    instances.add(message);
                }
            }
        }

        try {
            Path tmpFile = Files.createTempFile("zoi-shapes", ".json");
            JsonUtils.saveToFile(instances, tmpFile);
            RequestUtils.sendAttachment(response, tmpFile, "zoi-shapes.json");
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
