package org.hkijena.jast.repositories;

import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.model.entities.Image;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends CrudRepository<Image, Long> {
    List<Image> findBySeriesAndExperimentAndSample(TimeSeries timeSeries, String experiment, String sample);
    List<Image> findBySeries(TimeSeries timeSeries);
    List<Image> findByProject(Project project);
    int countByProject(Project project);
}
