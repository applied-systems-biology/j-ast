package org.hkijena.jast.repositories;

import org.hkijena.jast.model.entities.BackendTask;
import org.springframework.data.repository.CrudRepository;

public interface BackendTaskRepository extends CrudRepository<BackendTask, Long> {
}
