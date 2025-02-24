package org.hkijena.jast.controller;

import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.model.entities.Project;
import org.hkijena.jast.model.entities.User;
import org.hkijena.jast.payloads.ProjectMetadataPayload;
import org.hkijena.jast.payloads.UserPayload;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.repositories.UserRepository;
import org.hkijena.jast.services.UserService;
import org.hkijena.jast.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
public class AdminController {

    private final AccountConfig accountConfig;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @Autowired
    public AdminController(AccountConfig accountConfig, UserRepository userRepository, ProjectRepository projectRepository, PasswordEncoder passwordEncoder, UserService userService) {
        this.accountConfig = accountConfig;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    @GetMapping("/api/admin/list-users")
    public ResponseEntity<List<UserPayload>> listUsers(Authentication authentication) {
        userService.validateIsAdmin(authentication);
        List<UserPayload> result = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            result.add(new UserPayload(user));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/admin/list-projects/{userId}")
    public ResponseEntity<List<ProjectMetadataPayload>> listProjects(Authentication authentication, @PathVariable("userId") long userId) {
        userService.validateIsAdmin(authentication);
        ArrayList<ProjectMetadataPayload> result = new ArrayList<>();

        if (userId <= 0) {
            // Admin user
            for (Project project : projectRepository.findByOwnerIsNull()) {
                result.add(new ProjectMetadataPayload(project));
            }
        } else {
            Optional<User> byId = userRepository.findById(userId);
            if (byId.isPresent()) {
                for (Project project : projectRepository.findByOwner(byId.get())) {
                    result.add(new ProjectMetadataPayload(project));
                }
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/admin/edit-user")
    public ResponseEntity<String> editUser(Authentication authentication, @RequestBody UserPayload userPayload) {
        userService.validateIsAdmin(authentication);
        Optional<User> byId = userRepository.findById(userPayload.getId());
        if (byId.isPresent()) {
            User user = byId.get();
            if (!Objects.equals(user.getEmail(), userPayload.getEmail())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Inconsistent email. Cancelling!");
            }
            if (!StringUtils.isNullOrEmpty(userPayload.getNewPassword())) {
                if (!Objects.equals(userPayload.getNewPasswordConfirm(), userPayload.getNewPassword())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Inconsistent password confirmation.");
                }
                user.setPassword(passwordEncoder.encode(userPayload.getNewPassword()));
            }

            user.setAffiliation(userPayload.getAffiliation());
            user.setAllowLogin(userPayload.isAllowLogin());
            user.setFirstName(userPayload.getFirstName());
            user.setLastName(userPayload.getLastName());
            user.setRole(userPayload.getRole());
            user.setGuestExpire(LocalDateTime.now().plus(Duration.ofMinutes(accountConfig.getGuestAccountExpireMinutes())));
            userRepository.save(user);

            return ResponseEntity.ok("User was successfully edited.");
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

}
