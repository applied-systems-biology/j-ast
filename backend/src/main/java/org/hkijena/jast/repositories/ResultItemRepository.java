package org.hkijena.jast.repositories;

import org.hkijena.jast.model.entities.ResultItem;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultItemRepository extends CrudRepository<ResultItem, Long> {
}
