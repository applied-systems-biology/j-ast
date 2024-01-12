package org.hkijena.jipipe.webapp.growthassay.controller;

import org.hkijena.jipipe.webapp.growthassay.config.AccountConfig;
import org.hkijena.jipipe.webapp.growthassay.model.AdminPrincipal;
import org.hkijena.jipipe.webapp.growthassay.model.Privileges;
import org.hkijena.jipipe.webapp.growthassay.model.User;
import org.hkijena.jipipe.webapp.growthassay.model.UserPrincipal;
import org.hkijena.jipipe.webapp.growthassay.repositories.DatasetRepository;
import org.hkijena.jipipe.webapp.growthassay.services.DatasetService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class UserControllerAdvice {

    private final AccountConfig accountConfig;
    private final DatasetService datasetService;

    public UserControllerAdvice(AccountConfig accountConfig, DatasetService datasetService) {
        this.accountConfig = accountConfig;
        this.datasetService = datasetService;
    }

    @ModelAttribute("user")
    public User getUser(Authentication authentication) {
        if(authentication != null) {
            if(authentication.getPrincipal() instanceof AdminPrincipal) {
                User user = new User();
                user.setEmail(accountConfig.getAdminUsername());
                return user;
            }
            else if(authentication.getPrincipal() instanceof UserPrincipal) {
                return ((UserPrincipal) authentication.getPrincipal()).getUser();
            }
        }
        return null;
    }

    @ModelAttribute("canCreateProject")
    public boolean canCreateProject(Authentication authentication) {
        return datasetService.canCreateProject(authentication);
    }
}
