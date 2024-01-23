package org.hkijena.jipipe.webapp.growthassay.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import jakarta.persistence.*;

import java.io.Serial;

@Entity
@Table(name = "shape_presets")
public class ZOIShapePreset {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", columnDefinition = "TEXT")
    private String name = "";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description = "";

    @Column(name = "mca_intercept", nullable = false, columnDefinition = "DOUBLE")
    private double mcaIntercept = 6.120974538;

    @Column(name = "mca_rate", nullable = false, columnDefinition = "DOUBLE")
    private double mcaRate = -0.008732194;

    @Column(name = "strip_intercept", nullable = false, columnDefinition = "DOUBLE")
    private double stripIntercept = 0;

    @Column(name = "strip_rate", nullable = false, columnDefinition = "DOUBLE")
    private double stripRate = -0.008732194;

    @Column(name = "global", nullable = false)
    private boolean global = false;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

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
}
