package org.hkijena.jast.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PathUtils {
    public static List<Path> listFiles(Path directory) throws IOException {
        try(Stream<Path> stream = Files.list(directory)) {
            return stream.collect(Collectors.toList());
        }
    }

    public static void trySafeDeleteFile(Path file, Path parentStoragePath) {
        file = file.normalize().toAbsolutePath();
        parentStoragePath = parentStoragePath.normalize().toAbsolutePath();
        if(file.startsWith(parentStoragePath)) {
            try {
               Files.deleteIfExists(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            System.err.println("Tried to delete " + file + ", which is not a sub-path of " + parentStoragePath);
        }
    }

    public static Path resolveAndMakeSubDirectory(Path directory, String name) {
        return resolveAndMakeSubDirectory(directory, Paths.get(name));
    }

    public static Path resolveAndMakeSubDirectory(Path directory, Path name) {
        Path result = directory.resolve(name);
        if (!Files.exists(result)) {
            try {
                Files.createDirectories(result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    /**
     * Same as Files.createDirectories, but throws a {@link RuntimeException}
     * @param path the path
     * @return the path
     */
    public static Path createDirectories(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return path;
    }

    public static void createFileIfNotExists(Path filePath) {
        if(!Files.isRegularFile(filePath)) {
            try {
                Files.createFile(filePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
