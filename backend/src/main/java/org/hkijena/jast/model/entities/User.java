package org.hkijena.jast.model.entities;

import jakarta.persistence.*;
import org.hkijena.jast.utils.StringUtils;

import java.io.Serial;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(name = "email", unique = true, columnDefinition = "VARCHAR(320)")
    private String email = "";

    @Column(name = "first_name", columnDefinition = "TEXT")
    private String firstName = "";

    @Column(name = "last_name", columnDefinition = "TEXT")
    private String lastName = "";

    @Column(name = "affiliation", columnDefinition = "TEXT")
    private String affiliation = "";

    @Column(name = "password", columnDefinition = "TEXT")
    private String password = "";

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Role role = Role.User;

    @Column(name = "allow_login")
    private Boolean allowLogin = true;

    @Column(name = "guest_expire")
    private LocalDateTime guestExpire = LocalDateTime.now();

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public LocalDateTime getGuestExpire() {
        return guestExpire;
    }

    public void setGuestExpire(LocalDateTime guestExpire) {
        this.guestExpire = guestExpire;
    }

    public boolean isAllowLogin() {
        return allowLogin;
    }

    public void setAllowLogin(boolean locked) {
        this.allowLogin = locked;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return StringUtils.nullToEmpty(email);
    }

    public void setEmail(String username) {
        this.email = username;
    }

    public String getPassword() {
        return StringUtils.nullToEmpty(password);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return StringUtils.nullToEmpty(firstName);
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return StringUtils.nullToEmpty(lastName);
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Role getRole() {
        return role != null ? role : Role.User;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public enum Role {
        User,
        Guest,
        Admin
    }
}
