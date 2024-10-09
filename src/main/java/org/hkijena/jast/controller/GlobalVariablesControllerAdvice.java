package org.hkijena.jast.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.model.AdminPrincipal;
import org.hkijena.jast.model.entities.User;
import org.hkijena.jast.model.UserPrincipal;
import org.hkijena.jast.services.ProjectService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalVariablesControllerAdvice {

    private final AccountConfig accountConfig;
    private final ProjectService projectService;

    public GlobalVariablesControllerAdvice(AccountConfig accountConfig, ProjectService projectService) {
        this.accountConfig = accountConfig;
        this.projectService = projectService;
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

    @ModelAttribute("servletPath")
    public String getServletPath(HttpServletRequest request) {
        return request.getServletPath();
    }

    @ModelAttribute("canCreateProject")
    public boolean canCreateProject(Authentication authentication) {
        return projectService.canCreateProject(authentication);
    }
}
