package org.hkijena.jast.services;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jast.config.RuntimeConfig;
import org.hkijena.jast.model.ResultItemType;
import org.hkijena.jast.model.entities.*;
import org.hkijena.jast.payloads.task.BackendTaskPayload;
import org.hkijena.jast.repositories.BackendTaskRepository;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.repositories.ProjectRepository;
import org.hkijena.jast.tasks.BackendTaskWorkload;
import org.hkijena.jast.tasks.BackendTaskWorkloadParams;
import org.hkijena.jast.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BackendTaskUtils {

    public static final Logger LOGGER = LoggerFactory.getLogger(BackendTaskUtils.class);

    private final EntityManager entityManager;
    private final ProjectRepository projectRepository;
    private final BackendTaskService backendTaskService;
    private final BackendTaskRepository backendTaskRepository;

    @Autowired
    public BackendTaskUtils(EntityManager entityManager, ProjectRepository projectRepository, @Lazy BackendTaskService backendTaskService, BackendTaskRepository backendTaskRepository) {
        this.entityManager = entityManager;
        this.projectRepository = projectRepository;
        this.backendTaskService = backendTaskService;
        this.backendTaskRepository = backendTaskRepository;
    }

    public Path writeSharedFile(BackendTaskWorkloadParams params, Path sourcePath, Path targetPath) throws IOException {
        Path fullPath = Paths.get(params.getRuntimeConfig().getSharedResourcesDirectory()).resolve(sourcePath);
        Files.copy(fullPath, params.getTmpPath().resolve(targetPath));
        return params.getTmpPath().resolve(targetPath);
    }

    public Path writeSharedFile(BackendTaskWorkloadParams params, String sourcePath, String targetPath) throws IOException {
        return writeSharedFile(params, Paths.get(sourcePath), Paths.get(targetPath));
    }

    public Path writeSharedFile(BackendTaskWorkloadParams params, String sourcePath) throws IOException {
        return writeSharedFile(params, sourcePath, sourcePath);
    }

    public List<Map<String, String>> readCsv(BackendTaskWorkloadParams params, Path relativePath) throws IOException {
        return readCsv(params.getTmpPath().resolve(relativePath));
    }

    public List<Map<String, String>> readCsv(Path fullPath) throws IOException {
        try (FileReader reader = new FileReader(fullPath.toFile())) {
            CSVParser parser = new CSVParser(reader, CSVFormat.Builder.create().setHeader().setSkipHeaderRecord(true).setDelimiter(',').setQuote('"').build());
            return parser.getRecords().stream().map(CSVRecord::toMap).toList();
        }
    }

    public void writeRawImages(BackendTaskWorkloadParams params, Iterable<Long> imageIds, ImageRepository repository, FileStorageService fileStorageService, ProgressInfo progressInfo) throws IOException {
        Path rawPath = PathUtils.resolveAndMakeSubDirectory(params.getTmpPath(), "raw");
        Path csvPath = params.getTmpPath().resolve("metadata.csv");
        final String[] csvHeader = new String[]{"#ImageId", "#Experiment", "#Sample", "#TimePoint", "#AssayType", "PixelSize", "GroupRow", "GroupColumn"};
        try (FileWriter csvFileWriter = new FileWriter(csvPath.toFile())) {
            CSVPrinter csvPrinter = new CSVPrinter(csvFileWriter, CSVFormat.Builder.create().setDelimiter(',').setQuote('"').setHeader(csvHeader).build());
            ImmutableList<Image> images = ImmutableList.copyOf(repository.findAllById(imageIds));
            for (int i = 0; i < images.size(); i++) {
                progressInfo.setProgress(i, images.size());
                Image image = images.get(i);
                progressInfo.log("Writing raw image data " + image.getId());
                Path pngPath = rawPath.resolve(image.getId() + ".png");
                Files.write(pngPath, image.getRawData(fileStorageService));

                // Write metadata
                csvPrinter.printRecord(image.getId(), image.getExperiment(), image.getSample(), image.getTimePoint(), image.getAssayType(), image.getPixelSizeMillimeter(), image.getGroupRow(), image.getGroupColumn());
            }
        }
    }

    @Transactional
    public void writeMaskAnnotations(BackendTaskWorkloadParams params, Iterable<Long> imageIds, String annotationTypeId, ImageRepository repository, FileStorageService fileStorageService, ProgressInfo progressInfo) throws IOException {
        Path rawPath = PathUtils.resolveAndMakeSubDirectory(params.getTmpPath(), annotationTypeId);
        ImmutableList<Image> images = ImmutableList.copyOf(repository.findAllById(imageIds));
        for (int i = 0; i < images.size(); i++) {
            progressInfo.setProgress(i, images.size());
            Image image = images.get(i);
            progressInfo.log("Writing mask annotation " + annotationTypeId + " image data " + image.getId());
            MaskImageAnnotation annotation = image.getMaskImageAnnotation(annotationTypeId);
            Path pngPath = rawPath.resolve(image.getId() + ".png");
            if (annotation != null) {
                Files.write(pngPath, annotation.getRawData(fileStorageService));
            } else {
                BufferedImage dummy = new BufferedImage(image.getImageWidth(), image.getImageHeight(), BufferedImage.TYPE_BYTE_GRAY);
                ImageIO.write(dummy, "PNG", pngPath.toFile());
            }
        }
    }

    @Transactional
    public void writeRowFirstMaskAnnotations(BackendTaskWorkloadParams params, Iterable<Long> imageIds, String annotationTypeId, ImageRepository repository, FileStorageService fileStorageService, ProgressInfo progressInfo) throws IOException {
        Path rawPath = PathUtils.resolveAndMakeSubDirectory(params.getTmpPath(), annotationTypeId);
        List<Image> images = Lists.newArrayList(repository.findAllById(imageIds));
        images.sort(Comparator.comparing(Image::getGroupColumn));

        for (int groupRow : images.stream().map(Image::getGroupRow).collect(Collectors.toSet())) {
            progressInfo.log("Processing row " + groupRow);
            byte[] firstAnnotation = null;
            for (Image image : images) {
                if (image.getGroupRow() != groupRow) {
                    continue;
                }
                MaskImageAnnotation annotation = image.getMaskImageAnnotation(annotationTypeId);
                if (annotation != null) {
                    progressInfo.log("Found first available annotation " + annotationTypeId + " image data " + image.getId());
                    firstAnnotation = annotation.getRawData(fileStorageService);
                    break;
                }
            }
            for (Image image : images) {
                if (image.getGroupRow() != groupRow) {
                    continue;
                }
                progressInfo.log("Writing first available mask annotation " + annotationTypeId + " image data " + image.getId());
                MaskImageAnnotation annotation = image.getMaskImageAnnotation(annotationTypeId);
                Path pngPath = rawPath.resolve(image.getId() + ".png");
                if (firstAnnotation != null) {
                    Files.write(pngPath, firstAnnotation);
                } else if (annotation != null) {
                    Files.write(pngPath, annotation.getRawData(fileStorageService));
                } else {
                    BufferedImage dummy = new BufferedImage(image.getImageWidth(), image.getImageHeight(), BufferedImage.TYPE_BYTE_GRAY);
                    ImageIO.write(dummy, "PNG", pngPath.toFile());
                }
            }
        }
    }

    @Transactional
    public List<Image> readMaskAnnotations(Iterable<Long> imageIds, Map<String, Path> annotationTypeIdDirectories,
                                           ImageRepository imageRepository, FileStorageService fileStorageService, ProgressInfo progressInfo) throws IOException {
        List<Image> images = ImmutableList.copyOf(imageRepository.findAllById(imageIds));
        for (int i = 0; i < images.size(); i++) {
            progressInfo.setProgress(i, images.size());
            Image image = images.get(i);
            boolean changed = false;

            for (Map.Entry<String, Path> entry : annotationTypeIdDirectories.entrySet()) {
                String annotationTypeId = entry.getKey();
                Path imageDirectory = entry.getValue();
                Path imageFileName = imageDirectory.resolve(image.getId() + ".png");
                if (Files.isRegularFile(imageFileName)) {
                    progressInfo.log("Reading annotation from " + imageFileName);
                    try {
                        BufferedImage rawImage = ImageIO.read(imageFileName.toFile());

                        // Check the size
                        if (rawImage.getWidth() != image.getImageWidth() || rawImage.getHeight() != image.getImageHeight()) {
                            throw new IllegalArgumentException("Image dimensions do not match");
                        }

                        // Check if it's not empty
                        if(ImageUtils.isImageNotEmpty(rawImage)) {
                            // Get or create annotation and increment its version
                            MaskImageAnnotation annotation = image.getOrCreateMaskAnnotation(annotationTypeId, null);
                            annotation.setRawData(fileStorageService, ImageUtils.toPNGByteArray(rawImage));
                            annotation.setThumbnailData(fileStorageService, ImageUtils.toPNGByteArrayThumbnail(rawImage));
                            annotation.incrementVersion();

                            // Mark as changed
                            changed = true;
                        }
                        else {
                            progressInfo.log("Refusing to read annotation from " + imageFileName + ", as the image is empty!");
                        }

                    } catch (Throwable e) {
                        LOGGER.error("Unable to read annotation from {}", imageFileName, e);
                    }
                }
            }

            if (changed) {
                // Update the image as well
                image.rebuildThumbnail(fileStorageService);
                image.incrementVersion();
            }
        }

        imageRepository.saveAll(images);
        return images;
    }

    @Transactional
    public List<Image> readRawImages(Iterable<Long> imageIds, Path imageDirectory, ImageRepository imageRepository, FileStorageService fileStorageService, ProgressInfo progressInfo) throws IOException {
        List<Image> images = ImmutableList.copyOf(imageRepository.findAllById(imageIds));
        for (int i = 0; i < images.size(); i++) {
            progressInfo.setProgress(i, images.size());
            Image image = images.get(i);
            boolean changed = false;

            Path imageFileName = imageDirectory.resolve(image.getId() + ".png");
            if (Files.isRegularFile(imageFileName)) {
                progressInfo.log("Reading raw image from " + imageFileName);
                try {
                    BufferedImage rawImage = ImageIO.read(imageFileName.toFile());

                    // Check the size
                    if (rawImage.getWidth() != image.getImageWidth() || rawImage.getHeight() != image.getImageHeight()) {
                        throw new IllegalArgumentException("Image dimensions do not match");
                    }

                    image.setRawData(fileStorageService, ImageUtils.toPNGByteArray(rawImage));

                    // Mark as changed
                    changed = true;
                } catch (Throwable e) {
                    LOGGER.error("Unable to read annotation from {}", imageFileName, e);
                }
            }

            if (changed) {
                // Update the image as well
                image.rebuildThumbnail(fileStorageService);
                image.incrementVersion();
            }
        }

        imageRepository.saveAll(images);
        return images;
    }

    public ResultItem createResultItemFromPath(Path file, Path resultDirectory, FileStorageService fileStorageService, ProgressInfo progressInfo) {
        String path = StringUtils.nullToEmpty(resultDirectory.relativize(file).getParent());
        String name = file.getFileName().toString();

        ResultItem resultItem = new ResultItem();
        resultItem.setName(name);
        resultItem.setPath(path);

        progressInfo.log("Processing result " + path + "/" + name);

        if (name.endsWith(".png") || name.endsWith(".bmp") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            try {
                BufferedImage bufferedImage = ImageIO.read(file.toFile());
                resultItem.setRawData(fileStorageService, ImageUtils.toPNGByteArray(bufferedImage));
                resultItem.setThumbnailData(fileStorageService, ImageUtils.toPNGByteArrayThumbnail(bufferedImage));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            resultItem.setType(ResultItemType.Image);
            resultItem.setVisualizationType(ResultItemType.Null);
        } else if (name.endsWith(".csv")) {
            try {
                resultItem.setRawData(fileStorageService, Files.readAllBytes(file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            resultItem.setType(ResultItemType.Table);
            resultItem.setVisualizationType(ResultItemType.Null);
        } else if (name.endsWith(".txt") || name.endsWith(".json") || name.endsWith(".xml")) {
            try {
                resultItem.setRawData(fileStorageService, Files.readAllBytes(file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            resultItem.setType(ResultItemType.Text);
            resultItem.setVisualizationType(ResultItemType.Null);
        } else {
            try {
                resultItem.setRawData(fileStorageService, Files.readAllBytes(file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            resultItem.setType(ResultItemType.Unknown);
            resultItem.setVisualizationType(ResultItemType.Null);
        }

        return resultItem;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void readResultsDirectory(String resultName, String resultDescription, Path resultDirectory, Project project, ProjectRepository projectRepository, FileStorageService fileStorageService, ProgressInfo progressInfo) throws IOException {

        Result result = new Result();
        result.setName(StringUtils.orElse(resultName, "Result"));
        result.setDescription(StringUtils.nullToEmpty(resultDescription));


        FileVisitor<Path> visitor = new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                    ResultItem item = createResultItemFromPath(file, resultDirectory, fileStorageService, progressInfo);
                    if (item != null) {
                        result.addResultItem(item);
                    }
                } catch (Throwable e) {
                    progressInfo.log("Unable to read result file " + file);
                    progressInfo.log(ExceptionUtils.getStackTrace(e));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                progressInfo.log("Failed: " + file.toString());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }

                return FileVisitResult.CONTINUE;
            }
        };
        try {
            Files.walkFileTree(resultDirectory, visitor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        progressInfo.log("-> Discovered " + result.getResultItems().size() + " result items");
        progressInfo.log("Saving to database ...");
//        Result savedResult = resultRepository.save(result);
        project.addResult(result);
        projectRepository.save(project);
    }

    public void runJIPipe(BackendTaskWorkloadParams params, Path projectFile, Map<String, Object> parameterOverrides, String prefix, ProgressInfo progressInfo) {
        RuntimeConfig runtimeConfig = params.getRuntimeConfig();
        Path tmpPath = params.getTmpPath().toAbsolutePath().normalize();

        // Create a new JIPipe output directory
        Path outputDir = PathUtils.resolveAndMakeSubDirectory(params.getTmpPath(), prefix + "jip-output");

        // Write user directory overwrites
        Map<String, String> userDirectoriesConfig = new HashMap<>();
        userDirectoriesConfig.put("tmp_dir", tmpPath.toString());
        Path userDirectoryConfigJsonPath = params.getTmpPath().resolve(prefix + "jip-user-directories-config.json");
        JsonUtils.saveToFile(userDirectoriesConfig, userDirectoryConfigJsonPath);

        // Save parameter config
        Path parameterOverridesConfigJsonPath = null;
        if (parameterOverrides != null) {
            parameterOverridesConfigJsonPath = params.getTmpPath().resolve(prefix + "jip-parameter-config.json");
            JsonUtils.saveToFile(parameterOverrides, parameterOverridesConfigJsonPath);
        }

        // Run analysis
        ProgressInfo jipipeProgress = progressInfo.resolveAndLog("Running JIPipe");
        CommandLine commandLine;

        if (StringUtils.isNullOrEmpty(runtimeConfig.getFijiWrapper()) || !runtimeConfig.isFijiWrapperEnabled()) {
            Path jipipeExecutablePath = Path.of(runtimeConfig.getFijiExecutablePath());
            commandLine = new CommandLine(jipipeExecutablePath.toFile());
        } else {
            Path jipipeExecutablePath = Path.of(runtimeConfig.getFijiExecutablePath());
            commandLine = new CommandLine(Path.of(runtimeConfig.getFijiWrapper()).toFile());

            for (String arg : runtimeConfig.getFijiWrapperArgs()) {
                commandLine.addArgument(arg);
            }

            commandLine.addArgument(jipipeExecutablePath.toAbsolutePath().toString());
        }

        Path jipipeRootPath = Path.of(runtimeConfig.getFijiPath());
        progressInfo.incrementProgress();

        for (String arg : runtimeConfig.getFijiArgs()) {
            commandLine.addArgument(arg);
        }

        commandLine.addArgument("--pass-classpath");
        commandLine.addArgument("--full-classpath");
        commandLine.addArgument("--main-class");
        commandLine.addArgument("org.hkijena.jipipe.cli.JIPipeCLIMain");
        commandLine.addArgument("run");
        commandLine.addArgument("--project");
        commandLine.addArgument(projectFile.toAbsolutePath().toString());
        commandLine.addArgument("--output-folder");
        commandLine.addArgument(outputDir.toAbsolutePath().toString());
        commandLine.addArgument("--output-results");
        commandLine.addArgument("none");
        if (parameterOverridesConfigJsonPath != null) {
            commandLine.addArgument("--overwrite-parameters");
            commandLine.addArgument(parameterOverridesConfigJsonPath.toAbsolutePath().toString());
        }
        commandLine.addArgument("--overwrite-user-directories");
        commandLine.addArgument(userDirectoryConfigJsonPath.toAbsolutePath().toString());
        commandLine.addArgument("--fast-init");

        ProcessUtils.ExtendedExecutor executor = new ProcessUtils.ExtendedExecutor(ExecuteWatchdog.INFINITE_TIMEOUT, jipipeProgress, params.getLockFilePath());
        executor.setWorkingDirectory(jipipeRootPath.toFile());
        ProcessUtils.setupLogger(commandLine, executor, jipipeProgress);

        try {
            executor.execute(commandLine);
        } catch (Throwable e) {
            progressInfo.log("Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Schedules a new sub-task
     *
     * @param payload the payload for the subtask
     * @param registry       the task registry
     * @param progressInfo the progress info
     */
    @Transactional
    public void scheduleSubTask(BackendTaskPayload payload, BackendTaskRegistry registry, ProgressInfo progressInfo) throws Throwable {

        progressInfo.log("Scheduling new task " + payload.getTaskId());

        Optional<Project> project_ = projectRepository.findById(payload.getProjectId());
        if(project_.isEmpty()) {
            throw new RuntimeException("Project not found");
        }
        Project project = project_.get();

        BackendTaskWorkload taskWorkload = Objects.requireNonNull(registry.getTask(payload.getTaskId()));

        // We queue the payload into the system and let it be picked up by the task service automatically
        BackendTask task = new BackendTask();
        task.setName(taskWorkload.getName());
        task.setPayload(JsonUtils.toJsonString(payload));
        task.setProject(project);
        task.setTaskTypeId(payload.getTaskId());
        task.setTmpPath(backendTaskService.createTmpPath().toAbsolutePath().normalize().toString());
        task = backendTaskRepository.save(task);

        project.addTask(task);
        projectRepository.save(project);

        // Update the payload
        payload.setId(task.getId());

        // Immediately schedule the job
        backendTaskService.enqueueTask(task);
        task = backendTaskRepository.save(task);
    }

    public void clearTmp(Path tmpPath, ProgressInfo progressInfo) throws IOException {
        try (Stream<Path> files = Files.walk(tmpPath)) {
            files
                    .sorted(Comparator.reverseOrder()) // Ensure files are deleted before directories
                    .filter(path -> !path.getFileName().toString().equals("lockfile")) // Exclude lockfile
                    .filter(path -> !path.getFileName().toString().equals("job_started")) // Exclude lockfile
                    .filter(path -> !path.equals(tmpPath)) // Avoid deleting the tempDir itself
                    .forEach(path -> {
                        progressInfo.log("Deleting: " + path);
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            progressInfo.log("Failed to delete: " + path + " - " + e.getMessage());
                        }
                    });
        }
    }

//    /**
//     * Executes a subtask on the current params (will create a tmp subdirectory)
//     *
//     * @param params         the original params
//     * @param subTaskPayload the payload for the subtask
//     * @param registry       the task registry
//     * @param progressInfo the progress info
//     */
//    @Transactional
//    public void executeSubTask(BackendTaskWorkloadParams params, BackendTaskPayload subTaskPayload, BackendTaskRegistry registry, ProgressInfo progressInfo) throws Throwable {
//        BackendTaskWorkload taskWorkload = Objects.requireNonNull(registry.getTask(subTaskPayload.getTaskId()));
//        BackendTaskWorkloadParams derivedParams = new BackendTaskWorkloadParams();
//        derivedParams.setTmpPath(Files.createTempDirectory(params.getTmpPath(), subTaskPayload.getTaskId()));
//        derivedParams.setRuntimeConfig(params.getRuntimeConfig());
//        derivedParams.setLockFilePath(params.getTmpPath().resolve("lockfile"));
//        derivedParams.setPayload(subTaskPayload);
//
//        taskWorkload.execute(derivedParams, progressInfo.resolve(subTaskPayload.getTaskId()));
//    }

//    /**
//     * Forces the persistence of the provided entities immediately
//     * @param entities the entities
//     */
//    @Transactional
//    public void forceCommit(List<?> entities) {
//        for (Object entity : entities) {
//            entityManager.persist(entity);
//        }
//        entityManager.flush();
//        entityManager.clear();
//    }
}
