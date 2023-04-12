package org.hkijena.jipipe.growthassayanalyzerwebapp.utils;

public class StringUtils {

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
}
