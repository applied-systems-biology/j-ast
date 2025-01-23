package org.hkijena.jast.controller;

import jakarta.validation.Valid;
import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.model.UserPrincipal;
import org.hkijena.jast.model.entities.User;
import org.hkijena.jast.payloads.auth.UserAuthenticationLoginRequest;
import org.hkijena.jast.payloads.auth.UserAuthenticationLoginResponse;
import org.hkijena.jast.payloads.auth.UserAuthenticationRefreshRequest;
import org.hkijena.jast.payloads.auth.UserAuthenticationRefreshResponse;
import org.hkijena.jast.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;

@Controller
public class AuthenticationController {

    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final AccountConfig accountConfig;

    @Autowired
    public AuthenticationController(UserDetailsService userDetailsService, JwtUtil jwtUtil, AuthenticationManager authenticationManager, AccountConfig accountConfig) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.accountConfig = accountConfig;
    }

    @PostMapping("api/auth/login")
    public ResponseEntity<UserAuthenticationLoginResponse> login(@RequestBody @Valid UserAuthenticationLoginRequest request) {
        try {
            Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            UserDetails userDetails = (UserDetails) authenticate.getPrincipal();

            UserAuthenticationLoginResponse response = new UserAuthenticationLoginResponse();
            response.setUsername(userDetails.getUsername());

            LocalDateTime limit = tryExtractGuestLimit(userDetails);
            Date accessExpirationDate = jwtUtil.getNewLimitedAccessExpirationDate(limit);
            Date refreshExpirationDate = jwtUtil.getNewLimitedRefreshExpirationDate(limit);

            if (accessExpirationDate == null || refreshExpirationDate == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access expired");
            }

            response.setAccessToken(jwtUtil.generateAccessToken(userDetails.getUsername(), accessExpirationDate));
            response.setRefreshToken(jwtUtil.generateRefreshToken(userDetails.getUsername(), refreshExpirationDate));

            response.setGuestMaxProjects(accountConfig.getGuestProjectLimit());
            response.setGuestMaxImages(accountConfig.getGuestImageLimit());
            response.setGuestMaxExpireSeconds(accountConfig.getGuestAccountExpireMinutes() * 60L);

            if (userDetails instanceof UserPrincipal) {
                User user = ((UserPrincipal) userDetails).getUser();
                response.setRole(user.getRole());
                response.setGuestExpireSeconds(Duration.between(LocalDateTime.now(), user.getGuestExpire()).getSeconds());
            } else {
                response.setRole(User.Role.Admin);
            }
            response.setAuthorities(userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private LocalDateTime tryExtractGuestLimit(UserDetails userDetails) {
        LocalDateTime limit = null;
        if (userDetails instanceof UserPrincipal principal) {
            if (principal.getUser().getRole() == User.Role.Guest) {
                if (principal.isAccountNonExpired()) {
                    limit = principal.getUser().getGuestExpire();
                }
            }
        }
        return limit;
    }

    @PostMapping("api/auth/refresh")
    public ResponseEntity<UserAuthenticationRefreshResponse> refresh(@RequestBody @Valid UserAuthenticationRefreshRequest request) {
        try {
            if (!jwtUtil.isTokenExpired(request.getRefreshToken())) {

                String userName = jwtUtil.extractUsername(request.getRefreshToken(), true);
                String userName2 = jwtUtil.extractUsername(request.getAccessToken(), false);

                // Sanity check for the two tokens
                if (!userName.equals(userName2)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }

                // Check if we have the correct token types
                if (!jwtUtil.isAccessToken(request.getAccessToken(), false) || !jwtUtil.isRefreshToken(request.getRefreshToken(), true)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }

                // Get the principal
                UserDetails principal = userDetailsService.loadUserByUsername(userName);

                if (!principal.isAccountNonExpired() && !principal.isAccountNonLocked()) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
                }

                // Extract the refresh token's expiration date
                Date refreshExpirationDateFromToken = jwtUtil.extractExpiration(request.getRefreshToken(), true);
                LocalDateTime limit = tryExtractGuestLimit(principal);
                Date accessExpirationDate = jwtUtil.getNewLimitedAccessExpirationDate(limit);
                Date refreshExpirationDate = jwtUtil.getNewLimitedRefreshExpirationDate(limit);

                if (accessExpirationDate == null || refreshExpirationDate == null) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access expired");
                }

                // Update the expiration date if needed
                // To Prefer the smaller date
                if (!refreshExpirationDateFromToken.after(refreshExpirationDate)) {
                    refreshExpirationDate = refreshExpirationDateFromToken;
                }

                UserAuthenticationRefreshResponse response = new UserAuthenticationRefreshResponse();
                response.setRefreshToken(jwtUtil.generateRefreshToken(principal.getUsername(), refreshExpirationDate)); // Do not extend expiration
                response.setAccessToken(jwtUtil.generateAccessToken(principal.getUsername(), accessExpirationDate));
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
