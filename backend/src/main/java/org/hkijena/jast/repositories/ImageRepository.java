package org.hkijena.jast.repositories;

import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.model.entities.Image;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends CrudRepository<Image, Long> {
    List<Image> findByProject(Project project);
    int countByProject(Project project);
}
