package org.hkijena.jipipe.webapp.growthassay.services;

import com.google.common.collect.ImmutableList;
import jakarta.transaction.Transactional;
import org.hkijena.jipipe.webapp.growthassay.config.AccountConfig;
import org.hkijena.jipipe.webapp.growthassay.model.AdminPrincipal;
import org.hkijena.jipipe.webapp.growthassay.model.Dataset;
import org.hkijena.jipipe.webapp.growthassay.model.User;
import org.hkijena.jipipe.webapp.growthassay.model.UserPrincipal;
import org.hkijena.jipipe.webapp.growthassay.repositories.DatasetRepository;
import org.hkijena.jipipe.webapp.growthassay.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final AccountConfig accountConfig;
    private final UserRepository userRepository;
    private final DatasetRepository datasetRepository;
    private final DatasetService datasetService;
    private ApplicationContext applicationContext;
    private final SessionRegistry sessionRegistry;

    @Autowired
    public UserService(AccountConfig accountConfig, UserRepository userRepository, DatasetRepository datasetRepository, DatasetService datasetService, @Lazy SessionRegistry sessionRegistry) {
        this.accountConfig = accountConfig;
        this.userRepository = userRepository;
        this.datasetRepository = datasetRepository;
        this.datasetService = datasetService;
        this.sessionRegistry = sessionRegistry;
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
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if(principal instanceof UserPrincipal) {
                if(Objects.equals(((UserPrincipal) principal).getUser().getId(), user.getId())) {
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
        // Lock the user
        user.setAllowLogin(false);
        userRepository.save(user);

        // Stop all analyses of this user & erase data
        for (Dataset dataset : ImmutableList.copyOf(datasetRepository.findByOwner(user))) {
            dataset.tryCancelCurrentJob();
            dataset.setStatus(Dataset.Status.Preparing);
            dataset.clearOutputData();
            datasetRepository.save(dataset);
            datasetService.delete(dataset);
        }

        // Delete the user from the database
        userRepository.delete(user);

        // Logout user
        logoutUser(user);

    }

    @Scheduled(fixedRate = 60 * 1000)
    @Transactional
    public void autoDeleteGuests() {
        for (User user : ImmutableList.copyOf(userRepository.findAll())) {
            if(user.getRole() == User.Role.Guest) {
                if(user.getGuestExpire() == null || LocalDateTime.now().isAfter(user.getGuestExpire())) {
                    log.info("Deleting expired guest account " + user.getId() + " / " + user.getEmail());
                    deleteUser(user);
                }
            }
        }
    }
}
