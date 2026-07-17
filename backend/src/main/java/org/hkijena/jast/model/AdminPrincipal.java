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

package org.hkijena.jast.model;

import org.hkijena.jast.config.AccountConfig;
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
        return Privileges.ROLE_ADMIN_PRIVILEGES;
    }

    @Override
    public String getPassword() {
        return passwordEncoder.encode(accountConfig.getAdminPassword());
    }

    @Override
    public String getUsername() {
        return accountConfig.getAdminUsername();
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
