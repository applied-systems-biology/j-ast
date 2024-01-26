package org.hkijena.jipipe.webapp.growthassay.model;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.webapp.growthassay.utils.NaturalOrderComparator;
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
    @Column(name = "id")
    private Long id;

    @Column(name = "name", columnDefinition = "TEXT")
    @NotNull
    private String name = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    @Column(name = "storage_path", columnDefinition = "TEXT")
    @NotNull
    private String storagePath;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @NotNull
    private Status status = Status.Preparing;

    @Column(name = "growth_reduction_thresholds", columnDefinition = "TEXT")
    @NotNull
    private String growthReductionThresholds = "20, 50, 80";

    @Column(name = "time_point_early", columnDefinition = "TEXT")
    @NotNull
    private String timePointEarly = "";

    @Column(name = "time_point_late", columnDefinition = "TEXT")
    @NotNull
    private String timePointLate = "";

    @Column(name = "dda_disk_min_diameter", columnDefinition = "DOUBLE")
    @NotNull
    private double ddaDiskMinDiameter = 3;

    @Column(name = "dda_disk_max_diameter", columnDefinition = "DOUBLE")
    @NotNull
    private double ddaDiskMaxDiameter = 13;

    @Column(name = "dda_disk_min_circularity", columnDefinition = "DOUBLE")
    @NotNull
    private double ddaDiskMinCircularity = 0.5;

    @Column(name = "contrast_min_value", columnDefinition = "DOUBLE")
    @NotNull
    private double contrastMinValue = 50;
    @Column(name = "contrast_max_value", columnDefinition = "DOUBLE")
    @NotNull
    private double contrastMaxValue = 250;

    @Column(name = "ensure_circular_plate")
    @NotNull
    private boolean ensureCircularPlate = true;

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

    public String getGrowthReductionThresholds() {
        return growthReductionThresholds;
    }

    public void setGrowthReductionThresholds(String growthReductionThresholds) {
        this.growthReductionThresholds = StringUtils.nullToEmpty(growthReductionThresholds);
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

    public double getDdaDiskMinDiameter() {
        return ddaDiskMinDiameter;
    }

    public void setDdaDiskMinDiameter(double ddaDiskMinDiameter) {
        this.ddaDiskMinDiameter = ddaDiskMinDiameter;
    }

    public double getDdaDiskMaxDiameter() {
        return ddaDiskMaxDiameter;
    }

    public void setDdaDiskMaxDiameter(double ddaDiskMaxDiameter) {
        this.ddaDiskMaxDiameter = ddaDiskMaxDiameter;
    }

    public double getDdaDiskMinCircularity() {
        return ddaDiskMinCircularity;
    }

    public void setDdaDiskMinCircularity(double ddaDiskMinCircularity) {
        this.ddaDiskMinCircularity = ddaDiskMinCircularity;
    }

    public double getContrastMinValue() {
        return contrastMinValue;
    }

    public void setContrastMinValue(double contrastMinValue) {
        this.contrastMinValue = contrastMinValue;
    }

    public double getContrastMaxValue() {
        return contrastMaxValue;
    }

    public void setContrastMaxValue(double contrastMaxValue) {
        this.contrastMaxValue = contrastMaxValue;
    }

    public boolean isEnsureCircularPlate() {
        return ensureCircularPlate;
    }

    public void setEnsureCircularPlate(boolean ensureCircularPlate) {
        this.ensureCircularPlate = ensureCircularPlate;
    }

    public List<InputData> getInputData() {
        return inputData.stream().sorted(Comparator.comparing(InputData::getFinalFileName, NaturalOrderComparator.INSTANCE)).toList();
    }

    public List<OutputData> getOutputData() {
        return Collections.unmodifiableList(outputData);
    }

    public List<Double> tryParseGrowthReductionThresholds() {
        List<Double> result = new ArrayList<>();
        String str = StringUtils.nullToEmpty(getGrowthReductionThresholds()).trim();
        if (!StringUtils.isNullOrEmpty(str)) {
            for (String s : str.split(",")) {
                String item = s.trim();
                if (item.isEmpty()) {
                    return null;
                }
                if (NumberUtils.isCreatable(item)) {
                    result.add(NumberUtils.createDouble(item));
                } else {
                    return null;
                }
            }
        }
        return result;
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

    public boolean isEmpty() {
        return inputData.isEmpty();
    }

    public ValidationResult validate() {
        ValidationResult result = new ValidationResult();
        result.setValid(true);

        if (inputData.isEmpty()) {
            result.addIssue("No data", "Please upload data.");
        }
        Set<String> timePoints = new HashSet<>();
        boolean foundEmptyExperiments = false;
        boolean foundEmptyNames = false;
        boolean foundEmptyTimePoints = false;
        boolean foundInvalidPlateDiameters = false;
        Multiset<String> allNames = HashMultiset.create();
        Multiset<String> allNamesNoTimePoint = HashMultiset.create();
        Multiset<String> allNamesInvalidPlateDiameters = HashMultiset.create();
        Multiset<String> allNamesUnequalPlateDiameters = HashMultiset.create();
        Multiset<String> allNamesUnequalAssayTypes = HashMultiset.create();
        Multiset<String> allNamesUnequalZOIShape = HashMultiset.create();
        Set<String> invalidExperiments = new HashSet<>();
        Set<String> invalidSamples = new HashSet<>();
        for (InputData data : inputData) {
            if (StringUtils.isNullOrEmpty(data.getExperiment())) {
                foundEmptyExperiments = true;
            } else if (data.getExperiment().contains("_") || !StringUtils.isFilesystemCompatible(data.getExperiment())) {
                invalidExperiments.add(data.getExperiment());
            }
            if (StringUtils.isNullOrEmpty(data.getSample())) {
                foundEmptyNames = true;
            } else if (data.getSample().contains("_") || !StringUtils.isFilesystemCompatible(data.getSample())) {
                invalidSamples.add(data.getExperiment());
            }
            if (StringUtils.isNullOrEmpty(data.getTimePoint())) {
                foundEmptyTimePoints = true;
            } else {
                timePoints.add(data.getTimePoint());
            }
            if (data.getPlateDiameter() <= 0) {
                foundInvalidPlateDiameters = true;
                allNamesInvalidPlateDiameters.add(data.getExperiment() + "_" + data.getSample() + "_*");
            }
            allNames.add(data.getFinalFileName());
            allNamesNoTimePoint.add(data.getExperiment() + "_" + data.getSample() + "_*");
        }

        Map<String, List<InputData>> groups = inputData.stream().collect(Collectors.groupingBy(data -> data.getExperiment() + "_" + data.getSample()));
        for (Map.Entry<String, List<InputData>> entry : groups.entrySet()) {
            if (entry.getValue().size() == 2) {
                InputData first = entry.getValue().get(0);
                InputData second = entry.getValue().get(1);
                if (first.getPlateDiameter() != second.getPlateDiameter()) {
                    allNamesUnequalPlateDiameters.add(first.getExperiment() + "_" + first.getSample() + "_*");
                }
                if (first.getAssayType() != second.getAssayType()) {
                    allNamesUnequalAssayTypes.add(first.getExperiment() + "_" + first.getSample() + "_*");
                }
                if(first.isUsingAutoZOIShape() == second.isUsingAutoZOIShape()) {
                    if(first.isUsingAutoZOIShape()) {
                        if(first.geteTestZOIShapePresetMcaIntercept() != second.geteTestZOIShapePresetMcaIntercept() ||
                        first.geteTestZOIShapePresetMcaRate() != second.geteTestZOIShapePresetMcaRate() ||
                        first.geteTestZOIShapePresetStripIntercept() != second.geteTestZOIShapePresetStripIntercept() ||
                        first.geteTestZOIShapePresetStripRate() != second.geteTestZOIShapePresetStripRate()) {
                            allNamesUnequalZOIShape.add(first.getExperiment() + "_" + first.getSample() + "_*");
                        }
                    }
                }
                else {
                    allNamesUnequalZOIShape.add(first.getExperiment() + "_" + first.getSample() + "_*");
                }
            }
        }

        if (!allNamesUnequalPlateDiameters.isEmpty()) {
            result.addIssue("Unequal plate diameters", "Please ensure that images within the same experiment and sample have the same plate diameter. " +
                    "The following entries are affected: " + String.join(", ", allNamesUnequalPlateDiameters));
        }

        if (!allNamesUnequalAssayTypes.isEmpty()) {
            result.addIssue("Unequal assay types", "Please ensure that images within the same experiment and sample have the same assay type. " +
                    "The following entries are affected: " + String.join(", ", allNamesUnequalAssayTypes));
        }

        if (!allNamesUnequalZOIShape.isEmpty()) {
            result.addIssue("Unequal ZOI shape parameters", "Please ensure that images within the same experiment and sample have the ZOI shape parameters. " +
                    "Click the ZOI shape selection button in the data view to review the parameters. " +
                    "Assign the same preset for the two time points. " +
                    "If a preset is missing, create one using the ZOI Shape Presets view. " +
                    "The following entries are affected: " + String.join(", ", allNamesUnequalZOIShape));
        }

        if (timePoints.size() < 2) {
            result.addIssue("Too few time points", "Please ensure that you have exactly two time points.");
        } else if (timePoints.size() > 2) {
            result.addIssue("Too many time points", "Please ensure that you have exactly two time points.");
        }

        if (foundEmptyExperiments) {
            result.addIssue("Not all experiments set", "Please provide an experiment annotation to all inputs.");
        }
        if (foundEmptyNames) {
            result.addIssue("Not all samples set", "Please provide a sample to all inputs.");
        }
        if (foundEmptyTimePoints) {
            result.addIssue("Not all time points set", "Please provide a time point annotation (e.g., 24hr and 48hr) to all inputs.");
        }

        if (foundInvalidPlateDiameters) {
            result.addIssue("Invalid plate diameters", "Please ensure that all plate diameters are positive. The following entries are affected: " + String.join(", ", allNamesInvalidPlateDiameters));
        }

        if (!invalidExperiments.isEmpty()) {
            result.addIssue("Invalid experiments", "Please ensure that experiments do not have invalid characters (_, non-alphanumeric characters). " +
                    "The following entries are affected: " + String.join(", ", invalidExperiments));
        }
        if (!invalidSamples.isEmpty()) {
            result.addIssue("Invalid samples", "Please ensure that samples do not have invalid characters (_, non-alphanumeric characters). " +
                    "The following entries are affected: " + String.join(", ", invalidSamples));
        }

        List<String> duplicateElements = allNames.elementSet().stream().filter(element -> allNames.count(element) > 1).collect(Collectors.toList());
        if (!duplicateElements.isEmpty()) {
            result.addIssue("Duplicate elements", "Duplicates were found for the following entries: " + String.join(", ", duplicateElements));
        }

        List<String> wrongPairElements = allNamesNoTimePoint.elementSet().stream().filter(element -> allNamesNoTimePoint.count(element) != 2).collect(Collectors.toList());
        if (!wrongPairElements.isEmpty()) {
            result.addIssue("Wrong pairings", "Please ensure that exactly two time points are assigned to each (Experiment, Name) pair. The following entries are affected: " + String.join(", ", wrongPairElements));
        }

        if (!StringUtils.isNullOrEmpty(timePointEarly)) {
            if (!timePoints.contains(timePointEarly)) {
                result.addIssue("Time point not present in data", "Please ensure that 'Early time point' is set to one of the time points in the data table.");
            }
            if (timePointEarly.contains("_") || !StringUtils.isFilesystemCompatible(timePointEarly)) {
                result.addIssue("Invalid time point name", "The 'Early time point' name contains unsupported characters (_, non-alphanumeric characters)");
            }
        } else {
            result.addIssue("Early time point not configured", "Please ensure that 'Early time point' is set to one of the time points in the data table.");
        }

        if (!StringUtils.isNullOrEmpty(timePointLate)) {
            if (!timePoints.contains(timePointLate)) {
                result.addIssue("Time point not present in data", "Please ensure that 'Late time point' is set to one of the time points in the data table.");
            }
            if (timePointLate.contains("_") || !StringUtils.isFilesystemCompatible(timePointLate)) {
                result.addIssue("Invalid time point name", "The 'Late time point' name contains unsupported characters (_, non-alphanumeric characters)");
            }
        } else {
            result.addIssue("Late time point not configured", "Please ensure that 'Late time point' is set to one of the time points in the data table.");
        }

        List<Double> parsedThresholds = tryParseGrowthReductionThresholds();
        if (parsedThresholds != null && !parsedThresholds.isEmpty()) {
            for (Double threshold : parsedThresholds) {
                if (threshold <= 0 || threshold >= 100) {
                    result.addIssue("Invalid growth reduction threshold: " + threshold, "Please ensure that the value is between 0 and 100");
                }
            }
        } else {
            result.addIssue("Invalid growth reduction thresholds", "The set of growth reduction thresholds is empty or invalid. Use commas to provide multiple thresholds.");
        }

        if (ddaDiskMinDiameter > ddaDiskMaxDiameter) {
            result.addIssue("Invalid DDA disk diameter constraints", "The minimum value must be smaller than the maximum value");
        }

        if (contrastMinValue > contrastMaxValue) {
            result.addIssue("Invalid pixel value range", "Please ensure that the given pixel values are within [0, 255] and the minimum is not larger than the maximum.");
        }

        return result;
    }

    public void tryCancelCurrentJob() {
        if (status == Status.Running) {
            // Delete lockfile
            try {
                Files.deleteIfExists(Path.of(storagePath).resolve("lockfile"));
            } catch (IOException e) {
                e.printStackTrace();
            }

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

    public enum Status {
        Preparing,
        Running,
        RunFinished,
        RunInterrupted
    }
}
