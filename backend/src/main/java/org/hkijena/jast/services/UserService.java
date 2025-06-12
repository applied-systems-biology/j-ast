package org.hkijena.jast.services;

import com.google.common.collect.ImmutableList;
import jakarta.transaction.Transactional;
import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.config.WebSecurityConfig;
import org.hkijena.jast.model.AdminPrincipal;
import org.hkijena.jast.model.UserPrincipal;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.model.entities.User;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final AccountConfig accountConfig;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final SessionRegistry sessionRegistry;
    private ApplicationContext applicationContext;

    @Autowired
    public UserService(AccountConfig accountConfig, UserRepository userRepository, ProjectService projectService, @Lazy SessionRegistry sessionRegistry, ProjectRepository projectRepository) {
        this.accountConfig = accountConfig;
        this.userRepository = userRepository;
        this.projectService = projectService;
        this.sessionRegistry = sessionRegistry;
        this.projectRepository = projectRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        if (accountConfig.getAdminUsername().equalsIgnoreCase(username)) {
            return new AdminPrincipal(accountConfig, applicationContext.getBean(PasswordEncoder.class));
        }

        Optional<User> user = userRepository.findByEmailIgnoreCase(username);
        if (user.isEmpty()) {
            throw new UsernameNotFoundException(username);
        }
        return new UserPrincipal(user.get());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void logoutUser(User user) {
        if (accountConfig.isDisableAuth()) {
            return;
        }
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof UserPrincipal) {
                if (Objects.equals(((UserPrincipal) principal).getUser().getId(), user.getId())) {
                    List<SessionInformation> allSessions = sessionRegistry.getAllSessions(principal, false);
                    for (SessionInformation session : allSessions) {
                        session.expireNow();
                    }
                    return;
                }
            }
        }
    }

    public void deactivateUser(User user) {
        user.setAllowLogin(false);
        userRepository.save(user);
        logoutUser(user);
    }

    public void activateUser(User user) {
        user.setAllowLogin(true);
        userRepository.save(user);
    }

    public void deleteUser(User user) {

        if (accountConfig.isDisableAuth()) {
            log.info("User deletion is disabled (no auth)");
            return;
        }

        // Lock the user
        user.setAllowLogin(false);
        userRepository.save(user);

        // Stop all analyses of this user & erase data
        for (Project project : ImmutableList.copyOf(projectRepository.findByOwner(user))) {
            projectService.delete(project);
        }

        // Delete the user from the database
        userRepository.delete(user);

        // Logout user
        logoutUser(user);

    }

    @Scheduled(fixedRate = 60 * 1000)
    @Transactional
    public void autoDeleteGuests() {
        if (accountConfig.isDisableAuth()) {
            log.info("User deletion is disabled (no auth)");
            return;
        }

        for (User user : ImmutableList.copyOf(userRepository.findAll())) {
            if (user.getRole() == User.Role.Guest) {
                if (user.getGuestExpire() == null || LocalDateTime.now().isAfter(user.getGuestExpire())) {
                    log.info("Deleting expired guest account " + user.getId() + " / " + user.getEmail());
                    deleteUser(user);
                }
            }
        }
    }

    public void validateAuthentication(Authentication authentication) {
        if (accountConfig.isDisableAuth()) {
            log.info("Authentication is disabled (authentication granted)");
            return;
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public void validateIsAdmin(Authentication authentication) {

        if (accountConfig.isDisableAuth()) {
            log.info("Authentication is disabled (admin authentication granted)");
            return;
        }

        validateAuthentication(authentication);
        if (authentication.getPrincipal() instanceof AdminPrincipal) {
            // Everything OK
        } else if (authentication.getPrincipal() instanceof UserPrincipal) {
            if (((UserPrincipal) authentication.getPrincipal()).getUser().getRole() != User.Role.Admin) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public boolean isAdmin(Authentication authentication) {

        if (accountConfig.isDisableAuth()) {
            log.info("Authentication is disabled (admin authentication granted)");
            return true;
        }

        validateAuthentication(authentication);
        if (authentication.getPrincipal() instanceof AdminPrincipal) {
            return true;
        } else if (authentication.getPrincipal() instanceof UserPrincipal) {
            if (((UserPrincipal) authentication.getPrincipal()).getUser().getRole() != User.Role.Admin) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }
}
