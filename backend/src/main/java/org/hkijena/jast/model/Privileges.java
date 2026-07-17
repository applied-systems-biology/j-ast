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

import com.google.common.collect.Sets;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.HashSet;
import java.util.Set;

public class Privileges {

    public static final GrantedAuthority PRIVILEGE_USER = new SimpleGrantedAuthority("ROLE_USER");
    public static final GrantedAuthority PRIVILEGE_GUEST = new SimpleGrantedAuthority("ROLE_GUEST");
    public static final GrantedAuthority PRIVILEGE_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");
    public static final GrantedAuthority PRIVILEGE_CREATE_TASKS = new SimpleGrantedAuthority("CREATE_TASKS");
    public static final GrantedAuthority PRIVILEGE_VIEW_OWN_TASKS = new SimpleGrantedAuthority("VIEW_OWN_TASKS");
    public static final GrantedAuthority PRIVILEGE_VIEW_ALL_TASKS = new SimpleGrantedAuthority("VIEW_ALL_TASKS");
    public static final GrantedAuthority PRIVILEGE_EDIT_OWN_TASKS = new SimpleGrantedAuthority("EDIT_OWN_TASKS");
    public static final GrantedAuthority PRIVILEGE_EDIT_ALL_TASKS = new SimpleGrantedAuthority("EDIT_ALL_TASKS");
    public static final GrantedAuthority PRIVILEGE_CREATE_ACCOUNT = new SimpleGrantedAuthority("CREATE_ACCOUNT");
    public static final GrantedAuthority PRIVILEGE_DELETE_OWN_ACCOUNT = new SimpleGrantedAuthority("DELETE_OWN_ACCOUNT");
    public static final GrantedAuthority PRIVILEGE_DELETE_OTHER_ACCOUNT = new SimpleGrantedAuthority("DELETE_OTHER_ACCOUNT");
    public static final GrantedAuthority PRIVILEGE_EDIT_OWN_ACCOUNT = new SimpleGrantedAuthority("EDIT_OWN_ACCOUNT");
    public static final GrantedAuthority PRIVILEGE_EDIT_OTHER_ACCOUNT = new SimpleGrantedAuthority("EDIT_OTHER_ACCOUNT");

    public static final Set<GrantedAuthority> ROLE_USER_PRIVILEGES;

    public static final Set<GrantedAuthority> ROLE_ADMIN_PRIVILEGES;

    public static final HashSet<GrantedAuthority> ROLE_GUEST_PRIVILEGES;

    static {
        ROLE_USER_PRIVILEGES = Sets.newHashSet(PRIVILEGE_USER,
                PRIVILEGE_CREATE_TASKS,
                PRIVILEGE_VIEW_OWN_TASKS,
                PRIVILEGE_EDIT_OWN_TASKS,
                PRIVILEGE_DELETE_OWN_ACCOUNT,
                PRIVILEGE_EDIT_OWN_ACCOUNT);
        ROLE_GUEST_PRIVILEGES = Sets.newHashSet(PRIVILEGE_GUEST,
                PRIVILEGE_CREATE_TASKS,
                PRIVILEGE_VIEW_OWN_TASKS,
                PRIVILEGE_EDIT_OWN_TASKS,
                PRIVILEGE_DELETE_OWN_ACCOUNT);
        ROLE_ADMIN_PRIVILEGES = Sets.union(ROLE_USER_PRIVILEGES, Sets.newHashSet(
                PRIVILEGE_ADMIN,
                PRIVILEGE_VIEW_ALL_TASKS,
                PRIVILEGE_EDIT_ALL_TASKS,
                PRIVILEGE_CREATE_ACCOUNT,
                PRIVILEGE_DELETE_OTHER_ACCOUNT,
                PRIVILEGE_EDIT_OTHER_ACCOUNT
        ));
    }
}
