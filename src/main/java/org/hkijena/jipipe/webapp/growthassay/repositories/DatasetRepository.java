package org.hkijena.jipipe.webapp.growthassay.repositories;

import com.google.common.collect.Lists;
import org.hkijena.jipipe.webapp.growthassay.model.AdminPrincipal;
import org.hkijena.jipipe.webapp.growthassay.model.Dataset;
import org.hkijena.jipipe.webapp.growthassay.model.User;
import org.hkijena.jipipe.webapp.growthassay.model.UserPrincipal;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Repository;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Repository
public interface DatasetRepository extends CrudRepository<Dataset, Long> {

    List<Dataset> findByOwnerIsNull();

    List<Dataset> findByOwner(User owner);

    default void putSortedToModel(Model model, Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        ArrayList<Dataset> datasets;
        if(authentication.getPrincipal() instanceof UserPrincipal) {
            datasets = Lists.newArrayList(findByOwner(((UserPrincipal) authentication.getPrincipal()).getUser()));
        }
        else if(authentication.getPrincipal() instanceof AdminPrincipal) {
            datasets = Lists.newArrayList(findByOwnerIsNull());
        }
        else {
            throw new IllegalArgumentException("Unsupported principal type!");
        }
        datasets.sort(Comparator.comparing(Dataset::getName).reversed());
        model.addAttribute("datasets", datasets);
    }
}
