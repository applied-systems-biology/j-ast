package org.hkijena.jast.repositories;

import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.entities.MaskImageAnnotation;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MaskImageAnnotationRepository extends CrudRepository<MaskImageAnnotation, Long> {
    public Optional<MaskImageAnnotation> findFirstByImageAndType(Image image, String type);
}
