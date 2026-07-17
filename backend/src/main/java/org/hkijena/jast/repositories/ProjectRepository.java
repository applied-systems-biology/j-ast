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

package org.hkijena.jast.repositories;

import com.google.common.collect.ImmutableList;
import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.model.AdminPrincipal;
import org.hkijena.jast.model.UserPrincipal;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.model.entities.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.List;

public interface ProjectRepository extends CrudRepository<Project, Long> {
    List<Project> findByOwnerIsNull();

    List<Project> findByOwner(User owner);

    default Iterable<Project> getByAuthentication(Authentication authentication, AccountConfig accountConfig) {
        if (authentication == null || !authentication.isAuthenticated()) {
            if (accountConfig.isDisableAuth()) {
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
