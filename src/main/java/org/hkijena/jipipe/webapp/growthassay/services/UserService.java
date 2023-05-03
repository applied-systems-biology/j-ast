package org.hkijena.jipipe.webapp.growthassay.services;

import org.hkijena.jipipe.webapp.growthassay.config.AccountConfig;
import org.hkijena.jipipe.webapp.growthassay.model.AdminPrincipal;
import org.hkijena.jipipe.webapp.growthassay.model.User;
import org.hkijena.jipipe.webapp.growthassay.model.UserPrincipal;
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

import java.util.Optional;

@Service
public class UserService implements UserDetailsService, ApplicationContextAware {
    private final AccountConfig accountConfig;
    private final UserRepository userRepository;
    private ApplicationContext applicationContext;

    @Autowired
    public UserService(AccountConfig accountConfig, UserRepository userRepository) {
        this.accountConfig = accountConfig;
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        if(accountConfig.getAdminUserName().equals(username)) {
            return new AdminPrincipal(accountConfig, applicationContext.getBean(PasswordEncoder.class));
        }

        Optional<User> user = userRepository.findByEmailIgnoreCase(username);
        if(user.isEmpty()) {
            throw new UsernameNotFoundException(username);
        }
        return new UserPrincipal(user.get());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
