package org.hkijena.jipipe.webapp.growthassay.model;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import jakarta.persistence.*;
import org.hkijena.jipipe.webapp.growthassay.utils.StringUtils;
import org.jobrunr.scheduling.BackgroundJob;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "dataset")
    private List<OutputData> outputData = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public double getPercentageOfInhibition() {
        return percentageOfInhibition;
    }

    public void setPercentageOfInhibition(double percentageOfInhibition) {
        this.percentageOfInhibition = percentageOfInhibition;
    }

    public String getTimePointEarly() {
        return timePointEarly;
    }

    public void setTimePointEarly(String timePointEarly) {
        this.timePointEarly = timePointEarly;
    }

    public String getTimePointLate() {
        return timePointLate;
    }

    public void setTimePointLate(String timePointLate) {
        this.timePointLate = timePointLate;
    }

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

    public List<OutputData> getOutputData() {
        return Collections.unmodifiableList(outputData);
    }

    public void addInputData(InputData inputData) {
        this.inputData.add(inputData);
        inputData.setDataset(this);
    }

    public void removeInputData(InputData inputData) {
        this.inputData.remove(inputData);
        inputData.setDataset(null);
    }

    public void clearInputData() {
        for (InputData data : inputData) {
            data.setDataset(null);
        }
        inputData.clear();
    }

    public void addOutputData(OutputData outputData) {
        this.outputData.add(outputData);
        outputData.setDataset(this);
    }

    public void removeOutputData(OutputData outputData) {
        this.outputData.remove(outputData);
        outputData.setDataset(null);
    }

    public void clearOutputData() {
        for (OutputData data : outputData) {
            data.setDataset(null);
        }
        outputData.clear();
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
        Set<String> invalidExperiments = new HashSet<>();
        Set<String> invalidSamples = new HashSet<>();
        for (InputData data : inputData) {
            if(StringUtils.isNullOrEmpty(data.getExperiment())) {
                foundEmptyExperiments = true;
            }
            else if(data.getExperiment().contains("_") || !StringUtils.isFilesystemCompatible(data.getExperiment())) {
                invalidExperiments.add(data.getExperiment());
            }
            if(StringUtils.isNullOrEmpty(data.getSample())) {
                foundEmptyNames = true;
            }
            else if(data.getSample().contains("_") || !StringUtils.isFilesystemCompatible(data.getSample())) {
                invalidSamples.add(data.getExperiment());
            }
            if(StringUtils.isNullOrEmpty(data.getTimePoint())) {
                foundEmptyTimePoints = true;
            }
            else {
                timePoints.add(data.getTimePoint());
            }
            allNames.add(data.getFinalFileName());
            allNamesNoTimePoint.add(data.getExperiment() + "_" + data.getSample() + "_*");
        }

//        Map<String, List<InputData>> groups = inputData.stream().collect(Collectors.groupingBy(data -> data.getExperiment() + "_" + data.getSample()));
//        Set<String> unequalImageSizeData = new HashSet<>();
//        for (Map.Entry<String, List<InputData>> entry : groups.entrySet()) {
//            if(entry.getValue().size() == 2) {
//                InputData first = entry.getValue().get(0);
//                InputData second = entry.getValue().get(1);
//                if(first.getImageWidth() != second.getImageWidth() || first.getImageHeight() != second.getImageHeight()) {
//                    unequalImageSizeData.add(first.getFinalFileName());
//                    unequalImageSizeData.add(second.getFinalFileName());
//                }
//            }
//        }
//
//        if(!unequalImageSizeData.isEmpty()) {
//            result.addIssue("Unequal image sizes", "Please ensure that images within the same experiment and sample have the same size. " +
//                    "The following entries are affected: " + String.join(", ", unequalImageSizeData));
//        }

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
            result.addIssue("Not all samples set", "Please provide a sample to all inputs.");
        }
        if(foundEmptyTimePoints) {
            result.addIssue("Not all time points set", "Please provide a time point annotation (e.g., 24hr and 48hr) to all inputs.");
        }

        if(!invalidExperiments.isEmpty()) {
            result.addIssue("Invalid experiments", "Please ensure that experiments do not have invalid characters (_, non-alphanumeric characters). " +
                    "The following entries are affected: " + String.join(", ", invalidExperiments));
        }
        if(!invalidSamples.isEmpty()) {
            result.addIssue("Invalid samples", "Please ensure that samples do not have invalid characters (_, non-alphanumeric characters). " +
                    "The following entries are affected: " + String.join(", ", invalidSamples));
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
            if(timePointEarly.contains("_") || !StringUtils.isFilesystemCompatible(timePointEarly)) {
                result.addIssue("Invalid time point name", "The 'Early time point' name contains unsupported characters (_, non-alphanumeric characters)");
            }
        }
        else {
            result.addIssue("Early time point not configured", "Please ensure that 'Early time point' is set to one of the time points in the data table.");
        }

        if(!StringUtils.isNullOrEmpty(timePointLate)) {
            if(!timePoints.contains(timePointLate)) {
                result.addIssue("Time point not present in data", "Please ensure that 'Late time point' is set to one of the time points in the data table.");
            }
            if(timePointLate.contains("_") || !StringUtils.isFilesystemCompatible(timePointLate)) {
                result.addIssue("Invalid time point name", "The 'Late time point' name contains unsupported characters (_, non-alphanumeric characters)");
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

    public void tryCancelCurrentJob() {
        if(status == Status.Running) {
            // Read job id from a txt file
            Path jobIdFile = Path.of(storagePath).resolve("job-id.txt");
            if (Files.isRegularFile(jobIdFile)) {
                try {
                    String uuid = Files.readString(jobIdFile);
                    BackgroundJob.delete(uuid);
                    Files.delete(jobIdFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean canEdit(Authentication authentication) {

        if(authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if(authentication.getPrincipal() instanceof UserPrincipal) {
            return authentication.getPrincipal() == getOwner();
        }
        else if(authentication.getPrincipal() instanceof AdminPrincipal) {
            return getOwner() == null;
        }
        else {
            return authentication.getAuthorities().contains(Roles.PRIVILEGE_EDIT_ALL_TASKS);
        }
    }

    public boolean canAccess(Authentication authentication) {

        if(authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if(authentication.getPrincipal() instanceof UserPrincipal) {
            return authentication.getPrincipal() == getOwner();
        }
        else if(authentication.getPrincipal() instanceof AdminPrincipal) {
            return getOwner() == null;
        }
        else {
            return authentication.getAuthorities().contains(Roles.PRIVILEGE_VIEW_ALL_TASKS);
        }
    }

    public enum Status {
        Preparing,
        Running,
        RunFinished,
        RunInterrupted
    }
}
