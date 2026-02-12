package org.hkijena.jast.repositories;

import com.google.common.collect.ImmutableList;
import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.model.AdminPrincipal;
import org.hkijena.jast.model.UserPrincipal;
import org.hkijena.jast.model.entities.Preset;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.model.entities.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.List;

public interface PresetRepository extends CrudRepository<Preset, Long> {
    List<Preset> findByOwnerIsNull();

    List<Preset> findByOwner(User owner);

    default Iterable<Preset> getByAuthentication(Authentication authentication, AccountConfig accountConfig) {
        if (authentication == null || !authentication.isAuthenticated()) {
            if(accountConfig.isDisableAuth()) {
                // Just list all in the case of offline
                return ImmutableList.copyOf(findAll());
            }
            return Collections.emptyList();
        }
        if (authentication.getPrincipal() instanceof UserPrincipal) {
            return findByOwner(((UserPrincipal) authentication.getPrincipal()).getUser());
        } else if (authentication.getPrincipal() instanceof AdminPrincipal) {
            return findByOwnerIsNull();
        } else {
            throw new IllegalArgumentException("Unsupported principal type!");
        }
    }
}
