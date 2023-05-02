package org.hkijena.jipipe.webapp.growthassay.model;

import jakarta.persistence.*;

import java.io.Serial;

@Entity
@Table(name = "users")
public class User {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, columnDefinition = "VARCHAR(320)")
    private String email = "";

    @Column(name = "first_name", columnDefinition = "TEXT")
    private String firstName = "";

    @Column(name = "last_name", columnDefinition = "TEXT")
    private String lastName = "";

    @Column(name = "password", columnDefinition = "TEXT")
    private String password = "";

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role = Role.User;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String username) {
        this.email = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public enum Role {
        User,
        Admin
    }
}
