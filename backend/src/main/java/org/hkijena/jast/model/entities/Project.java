package org.hkijena.jast.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hkijena.jast.model.AdminPrincipal;
import org.hkijena.jast.model.Privileges;
import org.hkijena.jast.model.UserPrincipal;
import org.springframework.security.core.Authentication;

import java.io.Serial;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "projects")
public class Project {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", columnDefinition = "TEXT")
    @NotNull
    private String name = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "project")
    private List<Image> images = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public List<Image> getImages() {
        return images;
    }

    public @NotNull String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public void addImage(Image image) {
        images.add(image);
        image.setProject(this);
    }

    public void removeImage(Image image) {
        images.remove(image);
        image.setProject(null);
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public boolean isOwnedBy(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (authentication.getPrincipal() instanceof UserPrincipal) {
            return Objects.equals(((UserPrincipal) authentication.getPrincipal()).getUser().getId(), getOwner().getId());
        } else if (authentication.getPrincipal() instanceof AdminPrincipal) {
            return getOwner() == null;
        } else {
            return false;
        }
    }

    public boolean canEdit(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (authentication.getPrincipal() instanceof UserPrincipal) {
            return Objects.equals(((UserPrincipal) authentication.getPrincipal()).getUser().getId(), getOwner().getId());
        } else {
            return authentication.getAuthorities().contains(Privileges.PRIVILEGE_EDIT_ALL_TASKS);
        }
    }

    public boolean canAccess(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (authentication.getPrincipal() instanceof UserPrincipal) {
            return Objects.equals(((UserPrincipal) authentication.getPrincipal()).getUser().getId(), getOwner().getId());
        } else {
            return authentication.getAuthorities().contains(Privileges.PRIVILEGE_VIEW_ALL_TASKS);
        }
    }
}
