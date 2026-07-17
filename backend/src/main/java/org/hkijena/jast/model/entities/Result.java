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

package org.hkijena.jast.model.entities;

import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import org.hibernate.annotations.CreationTimestamp;
import org.hkijena.jast.services.FileStorageService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "results")
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name = "Unnamed";

    @Column(name = "description")
    private String description = "";

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    private Project project;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "viewed")
    private boolean viewed = false;

    @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "result")
    private List<ResultItem> resultItems = new ArrayList<>();

    public boolean isViewed() {
        return viewed;
    }

    public void setViewed(boolean viewed) {
        this.viewed = viewed;
    }

    public void addResultItem(ResultItem resultItem) {
        this.resultItems.add(resultItem);
        resultItem.setResult(this);
    }

    public void removeResultItem(ResultItem resultItem) {
        this.resultItems.remove(resultItem);
        resultItem.setResult(null);
    }

    public List<ResultItem> getResultItems() {
        return resultItems;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt != null ? createdAt : LocalDateTime.now();
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    @Transactional
    public void deleteFilesLater(FileStorageService fileStorageService) {
        for (ResultItem resultItem : getResultItems()) {
            resultItem.deleteFilesLater(fileStorageService);
        }
    }
}
