package org.hkijena.jipipe.webapp.growthassay.repositories;

import com.google.common.collect.Lists;
import org.hkijena.jipipe.webapp.growthassay.model.Privileges;
import org.hkijena.jipipe.webapp.growthassay.model.User;
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
        if(authentication != null && authentication.getAuthorities().contains(Privileges.PRIVILEGE_ADMIN)) {
            ArrayList<User> users = Lists.newArrayList(findAll());
            users.sort(Comparator.comparing(User::getRole).thenComparing(User::getEmail));
            model.addAttribute("allUsers", users);
        }
    }
}
