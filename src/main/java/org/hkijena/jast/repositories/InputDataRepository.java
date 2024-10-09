package org.hkijena.jast.repositories;

import org.hkijena.jast.model.Dataset;
import org.hkijena.jast.model.InputData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InputDataRepository extends CrudRepository<InputData, Long> {
    List<InputData> findByDatasetAndExperimentAndSample(Dataset dataset, String experiment, String sample);
    List<InputData> findByDataset(Dataset dataset);
}
