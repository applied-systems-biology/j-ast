package org.hkijena.jast.utils;

import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class StringUtils {

    public static final char[] INVALID_FILESYSTEM_CHARACTERS = new char[]{'<', '>', ':', '"', '/', '\\', '|', '?', '*', '{', '}'};

    /**
     * Returns true if the string is null or empty
     *
     * @param string the string
     * @return if the string is null or empty
     */
    public static boolean isNullOrEmpty(Object string) {
        if (string instanceof String)
            return ((String) string).isEmpty();
        else if (string != null)
            return ("" + string).isEmpty();
        else
            return true;
    }

    /**
     * Returns the string if its not null or empty or the alternative
     *
     * @param string        the string
     * @param ifNullOrEmpty returned if string is null or empty
     * @return string or ifNullOrEmpty depending on if string is null or empty
     */
    public static String orElse(Object string, String ifNullOrEmpty) {
        return isNullOrEmpty(string) ? ifNullOrEmpty : "" + string;
    }

    /**
     * Returns an empty string if s is null otherwise returns s
     *
     * @param s the string
     * @return an empty string if s is null otherwise returns s
     */
    public static String nullToEmpty(Object s) {
        return s == null ? "" : "" + s;
    }

    /**
     * Returns if the string does not contain invalid characters.
     * Assumes that the string is a filename, so path operators are not allowed.
     *
     * @param string the filename
     * @return if the filename is valid
     */
    public static boolean isFilesystemCompatible(String string) {
        for (char c : INVALID_FILESYSTEM_CHARACTERS) {
            if (string.contains(c + "")) {
                return false;
            }
        }
        return true;
    }

    /**
     * A nice human-readable format
     *
     * @param dateTime the time point
     * @return formatted string
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE) + " " +
                dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    /**
     * Replaces all characters invalid for filesystems with spaces
     * Assumes that the string is a filename, so path operators are not allowed.
     * Applies the limits for file / path names
     *
     * @param input filename
     * @return string compatible with file systems
     */
    public static String makeFilesystemCompatible(String input) {
        if (input == null)
            return null;
        for (char c : INVALID_FILESYSTEM_CHARACTERS) {
            input = input.replace(c, '-');
        }
        if (input.length() >= 255)
            input = input.substring(0, 255);
        return input;
    }

    /**
     * Converts a range string of format [range];[range];... to a list of integers
     *
     * @param value the range string
     * @return the list of integers
     */
    public static List<Integer> getIntegersFromRangeString(String value) {
        String string = StringUtils.orElse(value, "").replace(" ", "");
        List<Integer> integers = new ArrayList<>();
        string = string.replace(',', ';');
        for (String range : string.split(";")) {
            if (StringUtils.isNullOrEmpty(range))
                continue;
            if (range.contains("-")) {
                StringBuilder fromBuilder = new StringBuilder();
                StringBuilder toBuilder = new StringBuilder();
                boolean negative = false;
                boolean writeToFrom = true;
                for (int i = 0; i < range.length(); i++) {
                    char c = range.charAt(i);
                    if (c == '(') {
                        if (negative)
                            throw new NumberFormatException("Cannot nest brackets!");
                        negative = true;
                    } else if (c == ')') {
                        if (!negative)
                            throw new NumberFormatException("Cannot end missing start bracket!");
                        negative = false;
                    } else if (c == '-') {
                        if (negative) {
                            if (writeToFrom)
                                fromBuilder.append(c);
                            else
                                toBuilder.append(c);
                        } else {
                            if (!writeToFrom)
                                throw new RuntimeException("Additional hyphen detected!");
                            writeToFrom = false;
                        }
                    } else {
                        if (writeToFrom)
                            fromBuilder.append(c);
                        else
                            toBuilder.append(c);
                    }
                }

                // Parse borders
                int from = Integer.parseInt(fromBuilder.toString());
                int to = Integer.parseInt(toBuilder.toString());

                if (from <= to) {
                    for (int i = from; i <= to; ++i) {
                        integers.add(i);
                    }
                } else {
                    for (int i = to; i >= to; --i) {
                        integers.add(i);
                    }
                }
            } else {
                integers.add(Integer.parseInt(range));
            }
        }
        return integers;
    }

    public static List<String> readLastNLines(Path filePath, int n) throws Exception {
        List<String> result = new ArrayList<>();
        try (RandomAccessFile file = new RandomAccessFile(filePath.toFile(), "r")) {
            long fileLength = file.length() - 1;
            int linesRead = 0;
            StringBuilder line = new StringBuilder();

            for (long pointer = fileLength; pointer >= 0; pointer--) {
                file.seek(pointer);
                char c = (char) file.readByte();

                if (c == '\n' || pointer == 0) {
                    if (!line.isEmpty() || pointer == 0) {
                        if (pointer == 0 && c != '\n') {
                            line.append(c);
                        }
                        result.add(0, line.reverse().toString());
                        line = new StringBuilder();
                        linesRead++;
                        if (linesRead == n) break;
                    }
                } else {
                    line.append(c);
                }
            }
        }
        return result;
    }
}
