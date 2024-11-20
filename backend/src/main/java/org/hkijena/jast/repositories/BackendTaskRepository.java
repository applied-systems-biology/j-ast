package org.hkijena.jast.repositories;

import org.hkijena.jast.model.TaskStatus;
import org.hkijena.jast.model.entities.BackendTask;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface BackendTaskRepository extends CrudRepository<BackendTask, Long> {
    List<BackendTask> findAllByStatus(TaskStatus status);
}
