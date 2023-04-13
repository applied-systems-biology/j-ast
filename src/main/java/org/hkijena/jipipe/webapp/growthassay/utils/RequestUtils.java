package org.hkijena.jipipe.webapp.growthassay.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.Map;

public class RequestUtils {
    public static <T> T getObjectFromInputFlashMap(HttpServletRequest request, String key, Class<T> klass) {
        Map<String, ?> inputFlashMap = RequestContextUtils.getInputFlashMap(request);
        if(inputFlashMap != null) {
            Object value = inputFlashMap.getOrDefault(key, null);
            if(value != null && klass.isAssignableFrom(value.getClass())) {
                return (T) value;
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }
}
