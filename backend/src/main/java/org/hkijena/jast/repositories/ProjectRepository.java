package org.hkijena.jast.repositories;

import com.google.common.collect.Lists;
import org.hkijena.jast.model.AdminPrincipal;
import org.hkijena.jast.model.UserPrincipal;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.model.entities.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public interface ProjectRepository extends CrudRepository<Project, Long> {
    List<Project> findByOwnerIsNull();

    List<Project> findByOwner(User owner);

    default Iterable<Project> getByAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
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

    default void putSortedToModel(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        ArrayList<Project> timeSeries = Lists.newArrayList(getByAuthentication(authentication));
        timeSeries.sort(Comparator.comparing(Project::getName).reversed());
        model.addAttribute("projects", timeSeries);
    }
}
