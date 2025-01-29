package org.hkijena.jast.controller;

import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.model.AdminPrincipal;
import org.hkijena.jast.model.UserPrincipal;
import org.hkijena.jast.model.entities.User;
import org.hkijena.jast.payloads.UserPayload;
import org.hkijena.jast.payloads.register.UserRegistrationAllowedFeaturesPayload;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.repositories.UserRepository;
import org.hkijena.jast.services.UserService;
import org.hkijena.jast.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@RestController
public class AuthController {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AccountConfig accountConfig;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;


    @Autowired
    public AuthController(ProjectRepository projectRepository, UserRepository userRepository, AccountConfig accountConfig, PasswordEncoder passwordEncoder, UserService userService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.accountConfig = accountConfig;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    @GetMapping("/api/auth/registration-features")
    public ResponseEntity<UserRegistrationAllowedFeaturesPayload> getAllowedFeatures() {
        UserRegistrationAllowedFeaturesPayload payload = new UserRegistrationAllowedFeaturesPayload();
        payload.setAllowGuestAccounts(accountConfig.isAllowGuestAccounts());
        payload.setAllowSelfRegister(accountConfig.isAllowSelfRegister());
        payload.setGuestProjectLimit(accountConfig.getGuestProjectLimit());
        payload.setGuestImageLimit(accountConfig.getGuestImageLimit());
        payload.setAllowSelfRegister(accountConfig.isAllowSelfRegister());
        payload.setAdminContact(accountConfig.getAdminContact());
        return ResponseEntity.ok(payload);
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<String> register(@RequestBody UserPayload payload, Authentication authentication) {
        switch (payload.getRole()) {
            case Admin -> {
                // Only admins can register a new admin
                userService.validateIsAdmin(authentication);
            }
            case Guest -> {
                if (!accountConfig.isAllowGuestAccounts() && !userService.isAdmin(authentication)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }
            case User -> {
                if (!accountConfig.isAllowSelfRegister() && !userService.isAdmin(authentication)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        // Check E-mail
        if (StringUtils.isNullOrEmpty(payload.getEmail()) || payload.getEmail().contains(" ") || !payload.getEmail().contains("@") || accountConfig.getAdminUsername().equalsIgnoreCase(payload.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid E-Mail");
        }

        // Check if user already exists
        if (userRepository.existsByEmailIgnoreCase(payload.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists! Please contact " + accountConfig.getAdminContact() + " if you forgot your password.");
        }

        // Check password
        if (StringUtils.isNullOrEmpty(payload.getNewPassword()) || payload.getNewPassword().length() < 6 || !Objects.equals(payload.getNewPassword(), payload.getNewPasswordConfirm())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Password");
        }

        // Check other metadata
        if (StringUtils.isNullOrEmpty(payload.getFirstName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid first Name");
        }
        if (StringUtils.isNullOrEmpty(payload.getLastName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid last Name");
        }
        if (StringUtils.isNullOrEmpty(payload.getAffiliation())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid affiliation");
        }

        // Create the user
        User user = new User();
        user.setRole(payload.getRole());
        user.setEmail(payload.getEmail().toLowerCase());
        user.setPassword(passwordEncoder.encode(payload.getNewPassword()));
        user.setFirstName(payload.getFirstName());
        user.setLastName(payload.getLastName());
        user.setAffiliation(payload.getAffiliation());
        user.setGuestExpire(LocalDateTime.now().plus(Duration.ofMinutes(accountConfig.getGuestAccountExpireMinutes())));

        userRepository.save(user);

        return ResponseEntity.ok("Successfully registered user " + payload.getEmail() + " with role " + payload.getRole() + (payload.getRole() == User.Role.Guest ? " (Expires on " + user.getGuestExpire() + ")" : ""));
    }
}
