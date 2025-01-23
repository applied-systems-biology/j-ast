package org.hkijena.jast.utils;

import com.google.common.net.HttpHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.model.AdminPrincipal;
import org.hkijena.jast.model.UserPrincipal;
import org.hkijena.jast.model.entities.User;
import org.hkijena.jast.repositories.UserRepository;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtTokenFilter extends OncePerRequestFilter implements ApplicationContextAware {

    private final JwtUtil jwtUtil;
    private final AccountConfig accountConfig;
    private final UserRepository userRepository;
    private ApplicationContext applicationContext;

    @Autowired
    public JwtTokenFilter(JwtUtil jwtUtil, AccountConfig accountConfig, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.accountConfig = accountConfig;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        // Get authorization header and validate
        final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isNullOrEmpty(header) || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        // Get jwt token and validate
        final String token = header.split(" ")[1].trim();
        if (jwtUtil.isTokenExpired(token)) {
            chain.doFilter(request, response);
            return;
        }

        // Check if we have an access token
        if (!jwtUtil.isAccessToken(token, true)) {
            chain.doFilter(request, response);
            return;
        }

        // Get user identity and set it on the spring security context
        String username = jwtUtil.extractUsername(token, true);
        if (accountConfig.getAdminUsername().equals(username)) {
            // Admin authentication
            AdminPrincipal principal = new AdminPrincipal(accountConfig, applicationContext.getBean(PasswordEncoder.class));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null,
                    principal.getAuthorities()
            );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            // User authentication
            User user = userRepository
                    .findByEmailIgnoreCase(username)
                    .orElse(null);

            if (user == null) {
                // User not found
                chain.doFilter(request, response);
                return;
            }

            UserPrincipal principal = new UserPrincipal(user);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null,
                    principal.getAuthorities()
            );
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
