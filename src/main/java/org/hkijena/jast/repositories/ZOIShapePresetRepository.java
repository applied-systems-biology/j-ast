package org.hkijena.jast.repositories;

import org.hkijena.jast.model.AdminPrincipal;
import org.hkijena.jast.model.User;
import org.hkijena.jast.model.UserPrincipal;
import org.hkijena.jast.model.ZOIShapePreset;
import org.hkijena.jipipe.webapp.growthassay.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Repository
public interface ZOIShapePresetRepository extends JpaRepository<ZOIShapePreset, Long> {

    List<ZOIShapePreset> findByOwner(User owner);
    List<ZOIShapePreset> findByOwnerIsNull();
    List<ZOIShapePreset> findByGlobal(boolean global);

    default Iterable<ZOIShapePreset> getAvailableByAuthentication(Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated()) {
            return Collections.emptyList();
        }
        List<ZOIShapePreset> presets = new ArrayList<>();
        for (ZOIShapePreset preset : findAll()) {
            if(preset.isGlobal() || (authentication.getPrincipal() instanceof AdminPrincipal && preset.getOwner() == null) ||
                    (authentication.getPrincipal() instanceof UserPrincipal && preset.getOwner() != null && Objects.equals(preset.getOwner().getId(), ((UserPrincipal) authentication.getPrincipal()).getUser().getId()))) {
                presets.add(preset);
            }
        }
        return presets;
    }

    default Iterable<ZOIShapePreset> getReadonlyByAuthentication(Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated()) {
            return Collections.emptyList();
        }
        if(authentication.getPrincipal() instanceof UserPrincipal) {
            return findByGlobal(true);
        }
        else {
            return Collections.emptyList();
        }
    }

    default Iterable<ZOIShapePreset> getEditableByAuthentication(Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated()) {
            return Collections.emptyList();
        }
        if(authentication.getPrincipal() instanceof UserPrincipal) {
            return findByOwner(((UserPrincipal) authentication.getPrincipal()).getUser());
        }
        else if(authentication.getPrincipal() instanceof AdminPrincipal) {
            return findByOwnerIsNull();
        }
        else {
            throw new IllegalArgumentException("Unsupported principal type!");
        }
    }
}
