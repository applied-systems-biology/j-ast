package org.hkijena.jipipe.growthassayanalyzerwebapp.model;

import jakarta.persistence.*;

import java.io.Serial;

@Entity
@Table(name = "datasets")
public class DatasetModel {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;
}
