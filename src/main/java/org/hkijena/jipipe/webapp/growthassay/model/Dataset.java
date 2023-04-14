package org.hkijena.jipipe.webapp.growthassay.model;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import jakarta.persistence.*;
import org.hkijena.jipipe.webapp.growthassay.utils.StringUtils;

import java.io.Serial;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "datasets")
public class Dataset {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, columnDefinition = "TEXT")
    private String name = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    @Column(name = "storage_path", nullable = false, columnDefinition = "TEXT")
    private String storagePath;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status = Status.Preparing;

    @Column(name = "percentage_of_inhibition", nullable = false, columnDefinition = "DOUBLE")
    private double percentageOfInhibition = 50;

    @Column(name = "time_point_early", nullable = false, columnDefinition = "TEXT")
    private String timePointEarly = "";

    @Column(name = "time_point_late", nullable = false, columnDefinition = "TEXT")
    private String timePointLate = "";

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "dataset")
    private List<InputData> inputData = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<InputData> getInputData() {
        return Collections.unmodifiableList(inputData);
    }

    public void addInputData(InputData inputData) {
        this.inputData.add(inputData);
        inputData.setDataset(this);
    }

    public void removeInputData(InputData inputData) {
        this.inputData.remove(inputData);
        inputData.setDataset(null);
    }

    public ValidationResult validate() {
        ValidationResult result = new ValidationResult();
        result.setValid(true);

        if(inputData.isEmpty()) {
            result.addIssue("No data", "Please upload data.");
        }
        Set<String> timePoints = new HashSet<>();
        boolean foundEmptyExperiments = false;
        boolean foundEmptyNames = false;
        boolean foundEmptyTimePoints = false;
        Multiset<String> allNames = HashMultiset.create();
        Multiset<String> allNamesNoTimePoint = HashMultiset.create();
        for (InputData data : inputData) {
            if(StringUtils.isNullOrEmpty(data.getExperiment())) {
                foundEmptyExperiments = true;
            }
            if(StringUtils.isNullOrEmpty(data.getName())) {
                foundEmptyNames = true;
            }
            if(StringUtils.isNullOrEmpty(data.getTimePoint())) {
                foundEmptyTimePoints = true;
            }
            else {
                timePoints.add(data.getTimePoint());
            }
            allNames.add(data.getFinalFileName());
            allNamesNoTimePoint.add(data.getExperiment() + "_" + data.getName() + "_*");
        }

        if(timePoints.size() < 2) {
            result.addIssue("Too few time points", "Please ensure that you have exactly two time points.");
        }
        else if(timePoints.size() > 2) {
            result.addIssue("Too many time points", "Please ensure that you have exactly two time points.");
        }

        if(foundEmptyExperiments) {
            result.addIssue("Not all experiments set", "Please provide an experiment annotation to all inputs.");
        }
        if(foundEmptyNames) {
            result.addIssue("Not all names set", "Please provide a name to all inputs.");
        }
        if(foundEmptyTimePoints) {
            result.addIssue("Not all time points set", "Please provide a time point annotation (e.g., 24hr and 48hr) to all inputs.");
        }

        List<String> duplicateElements = allNames.elementSet().stream().filter(element -> allNames.count(element) > 1).collect(Collectors.toList());
        if(!duplicateElements.isEmpty()) {
            result.addIssue("Duplicate elements", "Duplicates were found for the following entries: " + String.join(", ", duplicateElements));
        }

        List<String> wrongPairElements = allNamesNoTimePoint.elementSet().stream().filter(element -> allNamesNoTimePoint.count(element) != 2).collect(Collectors.toList());
        if(!wrongPairElements.isEmpty()) {
            result.addIssue("Wrong pairings", "Please ensure that exactly two time points are assigned to each (Experiment, Name) pair. The following entries are affected: " + String.join(", ", wrongPairElements));
        }

        if(!StringUtils.isNullOrEmpty(timePointEarly)) {
            if(!timePoints.contains(timePointEarly)) {
                result.addIssue("Time point not present in data", "Please ensure that 'Early time point' is set to one of the time points in the data table.");
            }
        }
        else {
            result.addIssue("Early time point not configured", "Please ensure that 'Early time point' is set to one of the time points in the data table.");
        }

        if(!StringUtils.isNullOrEmpty(timePointLate)) {
            if(!timePoints.contains(timePointLate)) {
                result.addIssue("Time point not present in data", "Please ensure that 'Late time point' is set to one of the time points in the data table.");
            }
        }
        else {
            result.addIssue("Late time point not configured", "Please ensure that 'Late time point' is set to one of the time points in the data table.");
        }

        if(percentageOfInhibition <= 0 || percentageOfInhibition >= 100) {
            result.addIssue("Invalid percentage of inhibition", "Please ensure that the 'Percentage of inhibition'");
        }

        return result;
    }

    public enum Status {
        Preparing,
        Running,
        RunFinished,
        RunInterrupted
    }
}
