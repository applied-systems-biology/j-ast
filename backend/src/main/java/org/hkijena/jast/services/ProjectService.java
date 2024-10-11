package org.hkijena.jast.services;

import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.model.*;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.model.entities.User;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.repositories.ImageRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ImageRepository imageRepository;
    private final AccountConfig accountConfig;

    public ProjectService(ProjectRepository projectRepository, ImageRepository imageRepository, AccountConfig accountConfig) {
        this.projectRepository = projectRepository;
        this.imageRepository = imageRepository;
        this.accountConfig = accountConfig;
    }

    public boolean canCreateProject(Authentication authentication) {
        if(authentication != null) {
            if(authentication.getAuthorities().contains(Privileges.PRIVILEGE_CREATE_TASKS)) {
                if(authentication.getPrincipal() instanceof UserPrincipal) {
                    User user = ((UserPrincipal) authentication.getPrincipal()).getUser();
                    if(user.getRole() == User.Role.Guest) {
                        return projectRepository.findByOwner(user).size() < accountConfig.getGuestProjectLimit();
                    }
                    else {
                        return true;
                    }
                }
                else {
                    return true;
                }
            }
        }
        return false;
    }
    public void delete(Project project) {
        // Delete from database
        projectRepository.delete(project);
    }

    public boolean canUploadImage(Project project, Authentication authentication) {
        if(authentication != null) {
            if(authentication.getAuthorities().contains(Privileges.PRIVILEGE_CREATE_TASKS)) {
                if(authentication.getPrincipal() instanceof UserPrincipal) {
                    User user = ((UserPrincipal) authentication.getPrincipal()).getUser();
                    if(user.getRole() == User.Role.Guest) {
                        return imageRepository.countByProject(project) < accountConfig.getGuestImageLimit();
                    }
                    else {
                        return true;
                    }
                }
                else {
                    return true;
                }
            }
        }
        return false;
    }
}
