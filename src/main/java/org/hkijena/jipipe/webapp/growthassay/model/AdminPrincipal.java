package org.hkijena.jipipe.webapp.growthassay.model;

import org.hkijena.jipipe.webapp.growthassay.config.AccountConfig;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;

public class AdminPrincipal implements UserDetails {
    private final AccountConfig accountConfig;
    private final PasswordEncoder passwordEncoder;

    public AdminPrincipal(AccountConfig accountConfig, PasswordEncoder passwordEncoder) {
        this.accountConfig = accountConfig;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Roles.ROLE_ADMIN_PRIVILEGES;
    }

    @Override
    public String getPassword() {
        return passwordEncoder.encode(accountConfig.getAdminPassword());
    }

    @Override
    public String getUsername() {
        return accountConfig.getAdminUserName();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
