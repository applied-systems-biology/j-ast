package org.hkijena.jast.tasks;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.hkijena.jast.config.RuntimeConfig;
import org.hkijena.jast.model.AssayType;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.entities.MaskImageAnnotation;
import org.hkijena.jast.repositories.ImageRepository;
import org.hkijena.jast.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface BackendTaskWorkload {

    Logger LOGGER = LoggerFactory.getLogger(BackendTaskWorkload.class);

    String getName();

    String getDescription();

    BackendTaskWorkloadMode getMode();

    List<BackendTaskWorkloadDataSlot> getInputs();

    List<BackendTaskWorkloadDataSlot> getOutputs();

    List<BackendTaskWorkloadParameterSlot> getParameters();

    AssayType getAssayTypeRestriction();

    void execute(BackendTaskWorkloadParams params, ProgressInfo progressInfo) throws Throwable;

    default Path writeSharedFile(BackendTaskWorkloadParams params, Path sourcePath, Path targetPath) throws IOException {
        Path fullPath = Paths.get(params.getRuntimeConfig().getSharedResourcesDirectory()).resolve(sourcePath);
        Files.copy(fullPath, params.getTmpPath().resolve(targetPath));
        return params.getTmpPath().resolve(targetPath);
    }

    default Path writeSharedFile(BackendTaskWorkloadParams params, String sourcePath, String targetPath) throws IOException {
        return writeSharedFile(params, Paths.get(sourcePath), Paths.get(targetPath));
    }

    default Path writeSharedFile(BackendTaskWorkloadParams params, String sourcePath) throws IOException {
        return writeSharedFile(params, sourcePath, sourcePath);
    }

    default void writeRawImages(BackendTaskWorkloadParams params, Iterable<Long> imageIds, ImageRepository repository, ProgressInfo progressInfo) throws IOException {
        Path rawPath = PathUtils.resolveAndMakeSubDirectory(params.getTmpPath(), "raw");
        Path csvPath = params.getTmpPath().resolve("metadata.csv");
        final String[] csvHeader = new String[] { "#ImageId", "#Experiment", "#Sample", "#TimePoint", "#AssayType", "PixelSize" };
        try(FileWriter csvFileWriter = new FileWriter(csvPath.toFile())) {
            CSVPrinter csvPrinter = new CSVPrinter(csvFileWriter, CSVFormat.Builder.create().setDelimiter(',').setHeader(csvHeader).build());
            for (Image image : repository.findAllById(imageIds)) {
                progressInfo.log("Writing raw image data " + image.getId());
                Path pngPath = rawPath.resolve(image.getId() + ".png");
                Files.write(pngPath, image.getRawData());

                // Write metadata
                csvPrinter.printRecord(image.getId(), image.getExperiment(), image.getSample(), image.getTimePoint(), image.getAssayType(), image.getPixelSizeMillimeter());
            }
        }
    }

    default void writeMaskAnnotations(BackendTaskWorkloadParams params, Iterable<Long> imageIds, String annotationTypeId, ImageRepository repository, ProgressInfo progressInfo) throws IOException {
        Path rawPath = PathUtils.resolveAndMakeSubDirectory(params.getTmpPath(), annotationTypeId);
        for (Image image : repository.findAllById(imageIds)) {
            progressInfo.log("Writing mask annotation " + annotationTypeId + " image data " + image.getId());
            MaskImageAnnotation annotation = image.getMaskImageAnnotation(annotationTypeId);
            Path pngPath = rawPath.resolve(image.getId() + ".png");
            if(annotation != null) {
                Files.write(pngPath, annotation.getRawData());
            }
            else {
                BufferedImage dummy= new BufferedImage(image.getImageWidth(), image.getImageHeight(), BufferedImage.TYPE_BYTE_GRAY);
                ImageIO.write(dummy, "PNG", pngPath.toFile());
            }
        }
    }

    default void readMaskAnnotations(Iterable<Long> imageIds, Map<String, Path> annotationTypeIdDirectories,
                                     ImageRepository imageRepository, ProgressInfo progressInfo) throws IOException {
        Iterable<Image> images = imageRepository.findAllById(imageIds);
        for (Image image : images) {
            boolean changed = false;

            for (Map.Entry<String, Path> entry : annotationTypeIdDirectories.entrySet()) {
                String annotationTypeId = entry.getKey();
                Path imageDirectory = entry.getValue();
                Path imageFileName = imageDirectory.resolve(image.getId() + ".png");
                if(Files.isRegularFile(imageFileName)) {
                    progressInfo.log("Reading annotation from " + imageFileName);
                    try {
                        BufferedImage rawImage = ImageIO.read(imageFileName.toFile());

                        // Check the size
                        if(rawImage.getWidth() != image.getImageWidth() || rawImage.getHeight() != image.getImageHeight()) {
                            throw new IllegalArgumentException("Image dimensions do not match");
                        }

                        // Get or create annotation and increment its version
                        MaskImageAnnotation annotation = image.getOrCreateMaskAnnotation(annotationTypeId, null);
                        annotation.setRawData(ImageUtils.toPNGByteArray(rawImage));
                        annotation.setThumbnailData(ImageUtils.toPNGByteArrayThumbnail(rawImage));
                        annotation.incrementVersion();

                        // Mark as changed
                        changed = true;
                    }
                    catch (Throwable e) {
                        LOGGER.error("Unable to read annotation from {}", imageFileName, e);
                    }
                }
            }

            if(changed) {
                // Update the image as well
                image.rebuildThumbnail();
                image.incrementVersion();
            }
        }

        imageRepository.saveAll(images);
    }

    default void runJIPipe(BackendTaskWorkloadParams params, Path projectFile, Map<String, Object> parameterOverrides, String prefix, ProgressInfo progressInfo) {
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
        if(parameterOverrides != null) {
            parameterOverridesConfigJsonPath =  params.getTmpPath().resolve(prefix + "jip-parameter-config.json");
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
        if(parameterOverridesConfigJsonPath != null) {
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

}
