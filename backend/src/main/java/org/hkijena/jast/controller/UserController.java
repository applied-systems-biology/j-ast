/*
 * Copyright (c) 2026.
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jast.controller;

import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.model.AdminPrincipal;
import org.hkijena.jast.model.UserPrincipal;
import org.hkijena.jast.model.entities.User;
import org.hkijena.jast.payloads.UserPayload;
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

import java.util.Objects;

@RestController
public class UserController {

    private final UserRepository userRepository;
    private final AccountConfig accountConfig;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @Autowired
    public UserController(UserRepository userRepository, AccountConfig accountConfig, PasswordEncoder passwordEncoder, UserService userService) {
        this.userRepository = userRepository;
        this.accountConfig = accountConfig;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    @PostMapping("/api/current-user/update-metadata")
    public ResponseEntity<String> editUserMetadata(Authentication authentication, @RequestBody UserPayload userPayload) {

        if (authentication.getPrincipal() instanceof UserPrincipal) {
            User user = ((UserPrincipal) authentication.getPrincipal()).getUser();
            if (!Objects.equals(user.getEmail(), userPayload.getEmail())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Inconsistent email. Cancelling!");
            }

            user.setAffiliation(userPayload.getAffiliation());
            user.setFirstName(userPayload.getFirstName());
            user.setLastName(userPayload.getLastName());
            userRepository.save(user);

            return ResponseEntity.ok("User was successfully edited.");
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This user cannot be edited!");
    }

    @PostMapping("/api/current-user/update-password")
    public ResponseEntity<String> editUserPassword(Authentication authentication, @RequestBody UserPayload userPayload) {

        if (authentication.getPrincipal() instanceof UserPrincipal) {
            User user = ((UserPrincipal) authentication.getPrincipal()).getUser();
            if (!Objects.equals(user.getEmail(), userPayload.getEmail())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Inconsistent email. Cancelling!");
            }
            if (!StringUtils.isNullOrEmpty(userPayload.getNewPassword())) {
                if (!Objects.equals(userPayload.getNewPasswordConfirm(), userPayload.getNewPassword())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Inconsistent password confirmation.");
                }
                user.setPassword(passwordEncoder.encode(userPayload.getNewPassword()));
            }
            userRepository.save(user);

            return ResponseEntity.ok("User was successfully edited.");
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This user cannot be edited!");
    }


    @GetMapping("/api/current-user")
    public ResponseEntity<UserPayload> getInfo(Authentication authentication) {
        if (authentication.getPrincipal() instanceof UserPrincipal) {
            return ResponseEntity.ok(new UserPayload(((UserPrincipal) authentication.getPrincipal()).getUser()));
        } else if (authentication.getPrincipal() instanceof AdminPrincipal) {
            UserPayload payload = new UserPayload();
            payload.setEmail(accountConfig.getAdminUsername());
            payload.setRole(User.Role.Admin);
            return ResponseEntity.ok(payload);
        } else {
            return ResponseEntity.ok(new UserPayload());
        }
    }
}
