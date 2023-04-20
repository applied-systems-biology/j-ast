package org.hkijena.jipipe.webapp.growthassay.repositories;

import org.hkijena.jipipe.webapp.growthassay.model.Dataset;
import org.hkijena.jipipe.webapp.growthassay.model.InputData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InputDataRepository extends CrudRepository<InputData, Long> {
    List<InputData> findByDatasetAndExperimentAndSample(Dataset dataset, String experiment, String sample);
}
