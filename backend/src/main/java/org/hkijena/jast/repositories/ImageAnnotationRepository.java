package org.hkijena.jast.repositories;

import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.entities.ImageAnnotation;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImageAnnotationRepository extends CrudRepository<ImageAnnotation, Long> {
    public Optional<ImageAnnotation> findFirstByImageAndType(Image image, String type);
}
