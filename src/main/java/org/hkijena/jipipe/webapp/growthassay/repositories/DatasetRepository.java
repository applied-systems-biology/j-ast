package org.hkijena.jipipe.webapp.growthassay.repositories;

import com.google.common.collect.Lists;
import org.hkijena.jipipe.webapp.growthassay.model.Dataset;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.Comparator;

@Repository
public interface DatasetRepository extends CrudRepository<Dataset, Long> {
    default void putSortedToModel(Model model) {
        ArrayList<Dataset> datasets = Lists.newArrayList(findAll());
        datasets.sort(Comparator.comparing(Dataset::getName).reversed());
        model.addAttribute("datasets", datasets);
    }
}
