package org.hkijena.jast.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hkijena.jast.model.AdminPrincipal;
import org.hkijena.jast.model.Privileges;
import org.hkijena.jast.model.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import java.awt.*;
import java.io.Serial;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Entity
@Table(name = "projects")
public class Project {

    public static final Logger LOGGER = LoggerFactory.getLogger(Project.class);

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

    public List<Image> fixImageTableConsistency() {
        Set<Image> result = new HashSet<>();
        Map<Point, Image> locationMap = new HashMap<>();
        for (Image image : getImages()) {
            Point location = new Point(image.getGroupColumn(), Math.max(-1, image.getGroupRow()));
            if(locationMap.containsKey(location)) {
                // For unsorted rows, we don't care - the frontend will handle this
                // For sorted rows we kick duplicates back into unsorted
                if(image.getGroupRow() >= 0) {
                    LOGGER.info("Fixing duplicate assigment of image {} to {} by moving back to unsorted array", image.getId(), location);
                    image.setGroupRow(-1);
                    result.add(image);
                }
            }
            else {
                locationMap.put(location, image);
            }
        }
        return new ArrayList<>(result);
    }
}
