package org.hkijena.jast.controller;

import jakarta.validation.Valid;
import org.hkijena.jast.model.UserPrincipal;
import org.hkijena.jast.model.entities.User;
import org.hkijena.jast.model.messages.UserAuthenticationRequest;
import org.hkijena.jast.model.messages.UserAuthenticationResponse;
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

import java.time.Duration;
import java.time.LocalDateTime;

@Controller
public class AuthenticationController {

    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthenticationController(UserDetailsService userDetailsService, JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("api/login")
    public ResponseEntity<UserAuthenticationResponse> login(@RequestBody @Valid UserAuthenticationRequest request) {
        try {
            Authentication authenticate = authenticationManager.authenticate( new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            UserDetails userDetails = (UserDetails) authenticate.getPrincipal();

            UserAuthenticationResponse response = new UserAuthenticationResponse();
            response.setUsername(userDetails.getUsername());
            response.setToken(jwtUtil.generateToken(userDetails.getUsername()));
            if(userDetails instanceof UserPrincipal) {
                User user = ((UserPrincipal) userDetails).getUser();
                response.setRole(user.getRole());
                response.setGuestExpireSeconds(Duration.between(LocalDateTime.now(), user.getGuestExpire()).getSeconds());
            }
            else {
                response.setRole(User.Role.Admin);
            }
            response.setAuthorities(userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
