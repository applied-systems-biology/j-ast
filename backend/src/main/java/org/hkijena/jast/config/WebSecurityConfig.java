package org.hkijena.jast.config;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.hkijena.jast.utils.JwtTokenFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    public static final Logger LOG = LoggerFactory.getLogger(WebSecurityConfig.class);
    private final AccountConfig accountConfig;

    @Autowired
    public WebSecurityConfig(AccountConfig accountConfig) {
        this.accountConfig = accountConfig;
    }

    @Resource
    private UserDetailsService userDetailsService;

    @Resource
    private JwtTokenFilter jwtTokenFilter;

    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(authProvider());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        if (accountConfig.isDisableAuth()) {
            LOG.info("CORS configured for DESKTOP mode");
            config.addAllowedOrigin("*");
            config.addAllowedMethod("*");
            config.addAllowedHeader("*");
            config.setAllowCredentials(false);
        } else {
            LOG.info("CORS configured for WEB mode");
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Enable CORS and disable CSRF
        http.csrf(AbstractHttpConfigurer::disable);

        if(accountConfig.isDisableAuth()) {
            LOG.warn("Authentication disabled");
            http.cors(Customizer.withDefaults());
        }

        // Set session management to stateless
        http.sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Set unauthorized requests exception handler
        http.exceptionHandling(exception -> {
            exception.authenticationEntryPoint((request, response, authException) -> {
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        authException.getMessage()
                );
            });
        });

        // Configure permissions
        if(accountConfig.isDisableAuth()) {
            http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        }
        else {
            http.authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(AntPathRequestMatcher.antMatcher("/api/auth/**")).permitAll()
                    .requestMatchers(AntPathRequestMatcher.antMatcher("/api/test")).permitAll()
                    .requestMatchers(AntPathRequestMatcher.antMatcher("/api/ping")).permitAll()
                    .requestMatchers(AntPathRequestMatcher.antMatcher("/api/admin/**")).hasRole("ADMIN")
                    .requestMatchers(AntPathRequestMatcher.antMatcher("/api/**")).authenticated()
                    .anyRequest().permitAll());
        }


        // Add JWT token filter
        http.addFilterBefore(
                jwtTokenFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

//    // Used by Spring Security if CORS is enabled.
//    @Bean
//    public CorsFilter corsFilter() {
//        UrlBasedCorsConfigurationSource source =
//                new UrlBasedCorsConfigurationSource();
//        CorsConfiguration config = new CorsConfiguration();
//        config.setAllowCredentials(true);
//        config.addAllowedOrigin("*");
//        config.addAllowedHeader("*");
//        config.addAllowedMethod("*");
//        source.registerCorsConfiguration("/**", config);
//        return new CorsFilter(source);
//    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
}
