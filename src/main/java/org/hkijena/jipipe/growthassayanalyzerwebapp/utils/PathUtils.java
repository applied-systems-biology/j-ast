package org.hkijena.jipipe.growthassayanalyzerwebapp.utils;

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
}
