/*
 * Copyright (c) 2026.
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jast.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ArchiveUtils {
    private static final int BUFFER_SIZE = 8192;

    public static void zipFileOrDirectory(Path fileToZip, Path zipFile, ProgressInfo progressInfo) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            zipFile(fileToZip, fileToZip.getFileName().toString(), zipOut, progressInfo);
        }
    }

    public static void zipDirectory(Path rootPath, String rootPathName, Path zipFile, ProgressInfo progressInfo) throws IOException {
        if (!Files.isDirectory(rootPath)) {
            throw new IOException("Path is not a directory");
        }
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
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
            try (Stream<Path> stream = Files.list(fileToZip)) {
                List<Path> children = stream.toList();
                for (Path child : children) {
                    zipFile(child, fileName + "/" + child.getFileName(), zipOut, progressInfo);
                }
            }

            return;
        }
        progressInfo.log("ZIP " + fileToZip + " -> " + fileName);
        try (FileInputStream fis = new FileInputStream(fileToZip.toFile())) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[BUFFER_SIZE];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
        }
    }

    /**
     * Unzips a file
     *
     * @param zipFile      the zip file
     * @param targetDir    the target dir
     * @param progressInfo the progress info
     * @throws IOException io exception
     */
    public static void decompressZipFile(Path zipFile, Path targetDir, ProgressInfo progressInfo) throws IOException {
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            if (zipEntry.isDirectory()) {
                File newDirectory = decompressZipFileNewFile(targetDir.toFile(), zipEntry);
                progressInfo.log(newDirectory.toString());
                if (!Files.isDirectory(newDirectory.toPath()))
                    Files.createDirectories(newDirectory.toPath());
            } else {
                File newFile = decompressZipFileNewFile(targetDir.toFile(), zipEntry);
                progressInfo.log(newFile.toString());
                if (!Files.isDirectory(newFile.toPath().getParent()))
                    Files.createDirectories(newFile.toPath().getParent());
                if (Files.exists(newFile.toPath())) {
                    Files.delete(newFile.toPath());
                }
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    private static File decompressZipFileNewFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
