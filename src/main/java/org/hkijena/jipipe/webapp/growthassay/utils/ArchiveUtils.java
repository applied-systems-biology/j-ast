package org.hkijena.jipipe.webapp.growthassay.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ArchiveUtils {
    private static final int BUFFER_SIZE = 8192;

    public static void zipFileOrDirectory(Path fileToZip, Path zipFile, ProgressInfo progressInfo) throws IOException {
        try(ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            zipFile(fileToZip, fileToZip.getFileName().toString(), zipOut, progressInfo);
        }
    }

    public static void zipDirectory(Path rootPath, String rootPathName, Path zipFile, ProgressInfo progressInfo) throws IOException {
        if(!Files.isDirectory(rootPath)) {
            throw new IOException("Path is not a directory");
        }
        try(ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            zipFile(rootPath, rootPathName, zipOut, progressInfo);
        }
    }

    private static void zipFile(Path fileToZip, String fileName, ZipOutputStream zipOut, ProgressInfo progressInfo) throws IOException {
        if (Files.isHidden(fileToZip)) {
            return;
        }
        if (Files.isDirectory(fileToZip)) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            try(Stream<Path> stream = Files.list(fileToZip)) {
                List<Path> children = stream.toList();
                for (Path child : children) {
                    zipFile(child, fileName + "/" + child.getFileName(), zipOut, progressInfo);
                }
            }

            return;
        }
        progressInfo.log("ZIP " + fileToZip + " -> " + fileName);
        try(FileInputStream fis = new FileInputStream(fileToZip.toFile())) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[BUFFER_SIZE];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
        }
    }
}
