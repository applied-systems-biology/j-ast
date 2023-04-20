package org.hkijena.jipipe.webapp.growthassay.repositories;

import org.hkijena.jipipe.webapp.growthassay.model.OutputData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutputDataRepository extends JpaRepository<OutputData, Long> {
}
