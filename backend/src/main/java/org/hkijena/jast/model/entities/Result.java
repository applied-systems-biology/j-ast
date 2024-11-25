package org.hkijena.jast.model.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "results")
public class Result {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name = "Unnamed";

    @Column(name = "description")
    private String description = "";

    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "result")
    private List<ResultItem> resultItems = new ArrayList<>();

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
        return createdAt;
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
}
