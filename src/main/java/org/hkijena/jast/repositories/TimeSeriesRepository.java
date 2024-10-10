package org.hkijena.jast.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimeSeriesRepository extends CrudRepository<TimeSeries, Long> {
}
