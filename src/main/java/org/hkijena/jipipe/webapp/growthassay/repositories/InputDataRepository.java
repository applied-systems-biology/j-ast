package org.hkijena.jipipe.webapp.growthassay.repositories;

import org.hkijena.jipipe.webapp.growthassay.model.InputData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InputDataRepository extends CrudRepository<InputData, Long> {
}
