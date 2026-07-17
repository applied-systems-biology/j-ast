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

import com.google.common.collect.Lists;
import org.hkijena.jast.model.Privileges;
import org.hkijena.jast.model.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Repository;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    default void putSortedToModel(Model model, Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().contains(Privileges.PRIVILEGE_ADMIN)) {
            ArrayList<User> users = Lists.newArrayList(findAll());
            users.sort(Comparator.comparing(User::getRole).thenComparing(User::getEmail));
            model.addAttribute("allUsers", users);
        }
    }
}
