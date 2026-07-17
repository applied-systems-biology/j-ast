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

package org.hkijena.jast.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
    private boolean valid;

    private List<Issue> issues = new ArrayList<>();

    @JsonGetter("is-valid")
    public boolean isValid() {
        return valid;
    }

    @JsonSetter("is-valid")
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    @JsonGetter("issues")
    public List<Issue> getIssues() {
        return issues;
    }

    @JsonSetter("issues")
    public void setIssues(List<Issue> issues) {
        this.issues = issues;
    }

    public void addIssue(String title, String message) {
        valid = false;
        issues.add(new Issue(title, message));
    }

    public static class Issue {
        private String title;
        private String message;

        public Issue() {
        }

        public Issue(String title, String message) {
            this.title = title;
            this.message = message;
        }

        @JsonGetter("title")
        public String getTitle() {
            return title;
        }

        @JsonSetter("title")
        public void setTitle(String title) {
            this.title = title;
        }

        @JsonGetter("message")
        public String getMessage() {
            return message;
        }

        @JsonSetter("message")
        public void setMessage(String message) {
            this.message = message;
        }
    }
}
