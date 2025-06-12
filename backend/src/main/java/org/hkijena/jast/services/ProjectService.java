package org.hkijena.jast.services;

import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.model.Privileges;
import org.hkijena.jast.model.UserPrincipal;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.model.entities.User;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.repositories.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

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
        if (authentication != null) {
            if (authentication.getAuthorities().contains(Privileges.PRIVILEGE_CREATE_TASKS)) {
                if (authentication.getPrincipal() instanceof UserPrincipal) {
                    User user = ((UserPrincipal) authentication.getPrincipal()).getUser();
                    if (user.getRole() == User.Role.Guest) {
                        return projectRepository.findByOwner(user).size() < accountConfig.getGuestProjectLimit();
                    } else {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }

        // Desktop app mode
        if(accountConfig.isDisableAuth()) {
            return true;
        }

        return false;
    }

    public void delete(Project project) {
        // Delete from database
        projectRepository.delete(project);
    }

    public boolean canUploadImage(Project project, Authentication authentication) {
        if (authentication != null) {
            if (authentication.getAuthorities().contains(Privileges.PRIVILEGE_CREATE_TASKS)) {
                if (authentication.getPrincipal() instanceof UserPrincipal) {
                    User user = ((UserPrincipal) authentication.getPrincipal()).getUser();
                    if (user.getRole() == User.Role.Guest) {
                        return imageRepository.countByProject(project) < accountConfig.getGuestImageLimit();
                    } else {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }

        // Desktop app mode
        if(accountConfig.isDisableAuth()) {
            return true;
        }

        return false;
    }

    public Project getProjectByStringIdOrError(String id) {
        if (!NumberUtils.isCreatable(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        int projectId = Integer.parseInt(id);
        Optional<Project> project = projectRepository.findById((long) projectId);
        if (project.isPresent()) {
            return project.get();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    public Project getProjectByIdOrError(long projectId) {
        Optional<Project> project = projectRepository.findById(projectId);
        if (project.isPresent()) {
            return project.get();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
