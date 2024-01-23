package org.hkijena.jipipe.webapp.growthassay.repositories;

import org.hkijena.jipipe.webapp.growthassay.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public interface ZOIShapePresetRepository extends JpaRepository<ZOIShapePreset, Long> {

    List<ZOIShapePreset> findByOwner(User owner);
    List<ZOIShapePreset> findByOwnerIsNull();

    default Iterable<ZOIShapePreset> getByAuthentication(Authentication authentication) {
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
