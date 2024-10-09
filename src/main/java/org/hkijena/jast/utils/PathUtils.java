package org.hkijena.jast.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
}
