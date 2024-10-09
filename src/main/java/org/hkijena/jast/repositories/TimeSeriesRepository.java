package org.hkijena.jast.repositories;

import com.google.common.collect.Lists;
import org.hkijena.jast.model.AdminPrincipal;
import org.hkijena.jast.model.entities.TimeSeries;
import org.hkijena.jast.model.entities.User;
import org.hkijena.jast.model.UserPrincipal;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Repository;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Repository
public interface TimeSeriesRepository extends CrudRepository<TimeSeries, Long> {
}
