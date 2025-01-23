package org.hkijena.jast.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class RequestUtils {
    public static <T> T getObjectFromInputFlashMap(HttpServletRequest request, String key, Class<T> klass) {
        Map<String, ?> inputFlashMap = RequestContextUtils.getInputFlashMap(request);
        if (inputFlashMap != null) {
            Object value = inputFlashMap.getOrDefault(key, null);
            if (value != null && klass.isAssignableFrom(value.getClass())) {
                return (T) value;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static void sendAttachment(HttpServletResponse response, Path absoluteCurrentEntryPath, String fileName) throws IOException {
        response.setContentType(Files.probeContentType(absoluteCurrentEntryPath));
        response.setHeader("Content-Length", Long.toString(Files.size(absoluteCurrentEntryPath)));
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        var ins = new FileInputStream(absoluteCurrentEntryPath.toFile());
        IOUtils.copy(ins, response.getOutputStream());
        IOUtils.closeQuietly(ins);
        IOUtils.closeQuietly(response.getOutputStream());
    }

    public static void sendAttachment(HttpServletResponse response, byte[] data, String fileName, String contentType) throws IOException {
        response.setContentType(contentType);
        response.setHeader("Content-Length", Long.toString(data.length));
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        var ins = new ByteArrayInputStream(data);
        IOUtils.copy(ins, response.getOutputStream());
        IOUtils.closeQuietly(ins);
        IOUtils.closeQuietly(response.getOutputStream());
    }

    public static void sendContent(HttpServletResponse response, Path absoluteCurrentEntryPath) throws IOException {
        response.setContentType(Files.probeContentType(absoluteCurrentEntryPath));
        response.setHeader("Content-Length", Long.toString(Files.size(absoluteCurrentEntryPath)));

        var ins = new FileInputStream(absoluteCurrentEntryPath.toFile());
        IOUtils.copy(ins, response.getOutputStream());
        IOUtils.closeQuietly(ins);
        IOUtils.closeQuietly(response.getOutputStream());
    }

    public static void sendContent(HttpServletResponse response, byte[] data, String contentType) throws IOException {
        response.setContentType(contentType);
        response.setHeader("Content-Length", Long.toString(data.length));

        var ins = new ByteArrayInputStream(data);
        IOUtils.copy(ins, response.getOutputStream());
        IOUtils.closeQuietly(ins);
        IOUtils.closeQuietly(response.getOutputStream());
    }
}
