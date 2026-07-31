/*
 * Copyright (c) 2026.
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jast.repositories;

import org.hkijena.jast.model.DownloadBundleStatus;
import org.hkijena.jast.model.entities.DownloadBundle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DownloadBundleRepository extends JpaRepository<DownloadBundle, String> {
    List<DownloadBundle> findByExpiresAtBefore(LocalDateTime dateTime);
    List<DownloadBundle> findByStatus(DownloadBundleStatus status);

    @Query("SELECT DISTINCT b FROM DownloadBundle b JOIN FETCH b.result r LEFT JOIN FETCH r.resultItems WHERE b.id = :id")
    Optional<DownloadBundle> findByIdWithData(@Param("id") String id);
}
