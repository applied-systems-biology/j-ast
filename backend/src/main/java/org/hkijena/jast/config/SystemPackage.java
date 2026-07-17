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

package org.hkijena.jast.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SystemPackage {
    private String operatingSystem;
    private List<String> key;
    private String value;
    private String mustExist;

    public String getMustExist() {
        return mustExist;
    }

    public void setMustExist(String mustExist) {
        this.mustExist = mustExist;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public List<String> getKey() {
        return key;
    }

    public void setKey(List<String> key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isPresent() {
        boolean osCompatible = switch (operatingSystem) {
            case "linux" -> SystemUtils.IS_OS_LINUX;
            case "windows" -> SystemUtils.IS_OS_WINDOWS;
            case "macos" -> SystemUtils.IS_OS_MAC;
            default -> false;
        };
        if (osCompatible) {
            if (!StringUtils.isBlank(mustExist)) {
                return Files.exists(Path.of(mustExist));
            }
            return true;
        }
        return false;
    }
}
