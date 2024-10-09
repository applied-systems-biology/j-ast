package org.hkijena.jast.repositories;

import com.google.common.collect.Lists;
import org.hkijena.jast.model.AdminPrincipal;
import org.hkijena.jast.model.Dataset;
import org.hkijena.jast.model.User;
import org.hkijena.jast.model.UserPrincipal;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Repository;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Repository
public interface DatasetRepository extends CrudRepository<Dataset, Long> {

    List<Dataset> findByOwnerIsNull();

    List<Dataset> findByOwner(User owner);

    default Iterable<Dataset> getByAuthentication(Authentication authentication) {
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

    default void putSortedToModel(Model model, Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        ArrayList<Dataset> datasets = Lists.newArrayList(getByAuthentication(authentication));
        datasets.sort(Comparator.comparing(Dataset::getName).reversed());
        model.addAttribute("datasets", datasets);
    }
}
