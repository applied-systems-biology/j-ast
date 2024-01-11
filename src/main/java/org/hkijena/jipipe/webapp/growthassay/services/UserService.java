package org.hkijena.jipipe.webapp.growthassay.services;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.webapp.growthassay.config.AccountConfig;
import org.hkijena.jipipe.webapp.growthassay.model.AdminPrincipal;
import org.hkijena.jipipe.webapp.growthassay.model.Dataset;
import org.hkijena.jipipe.webapp.growthassay.model.User;
import org.hkijena.jipipe.webapp.growthassay.model.UserPrincipal;
import org.hkijena.jipipe.webapp.growthassay.repositories.DatasetRepository;
import org.hkijena.jipipe.webapp.growthassay.repositories.UserRepository;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService, ApplicationContextAware {
    private final AccountConfig accountConfig;
    private final UserRepository userRepository;
    private final DatasetRepository datasetRepository;
    private final DatasetService datasetService;
    private ApplicationContext applicationContext;

    @Autowired
    public UserService(AccountConfig accountConfig, UserRepository userRepository, DatasetRepository datasetRepository, DatasetService datasetService) {
        this.accountConfig = accountConfig;
        this.userRepository = userRepository;
        this.datasetRepository = datasetRepository;
        this.datasetService = datasetService;
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
    }
}
