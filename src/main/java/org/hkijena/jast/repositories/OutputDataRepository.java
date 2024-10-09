package org.hkijena.jast.repositories;

import org.hkijena.jast.model.OutputData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutputDataRepository extends JpaRepository<OutputData, Long> {
}
