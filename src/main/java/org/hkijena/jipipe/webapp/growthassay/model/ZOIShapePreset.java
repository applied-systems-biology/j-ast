package org.hkijena.jipipe.webapp.growthassay.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hkijena.jipipe.webapp.growthassay.utils.ColorUtils;

import java.awt.*;
import java.io.Serial;
import java.util.Objects;

@Entity
@Table(name = "shape_presets")
public class ZOIShapePreset {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", columnDefinition = "TEXT")
    private String name = "";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description = "";

    @Column(name = "mca_intercept", columnDefinition = "DOUBLE")
    @NotNull
    private double mcaIntercept = 6.120974538;

    @Column(name = "mca_rate", columnDefinition = "DOUBLE")
    @NotNull
    private double mcaRate = -0.008732194;

    @Column(name = "strip_intercept", columnDefinition = "DOUBLE")
    @NotNull
    private double stripIntercept = 0;

    @Column(name = "strip_rate", columnDefinition = "DOUBLE")
    @NotNull
    private double stripRate = -0.008732194;

    @Column(name = "global")
    @NotNull
    private boolean global = false;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    public String getHexColor() {
        if(mcaRate == 0 && mcaIntercept == 0 && stripRate == 0 && stripIntercept == 0) {
            return ColorUtils.colorToHexString(new Color(0xF8F9FA));
        }
        int hash = Objects.hash(mcaIntercept, mcaRate, stripIntercept, stripRate);
        float hue = (float) ((hash & 0xFFFF) % 360) / 360.0f;
        Color hsbColor = Color.getHSBColor(hue, 75.91f / 100f, 86.27f / 100f);
        return ColorUtils.colorToHexString(hsbColor);
    }

    public boolean isGlobal() {
        return global;
    }

    public void setGlobal(boolean global) {
        this.global = global;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getMcaIntercept() {
        return mcaIntercept;
    }

    public void setMcaIntercept(double mcaIntercept) {
        this.mcaIntercept = mcaIntercept;
    }

    public double getMcaRate() {
        return mcaRate;
    }

    public void setMcaRate(double mcaRate) {
        this.mcaRate = mcaRate;
    }

    public double getStripIntercept() {
        return stripIntercept;
    }

    public void setStripIntercept(double stripIntercept) {
        this.stripIntercept = stripIntercept;
    }

    public double getStripRate() {
        return stripRate;
    }

    public void setStripRate(double stripRate) {
        this.stripRate = stripRate;
    }

    public String getOwnerName() {
        if(getOwner() == null) {
            return "Admin";
        }
        else {
            return getOwner().getEmail();
        }
    }
}
