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

import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class Notification implements Serializable {

    public static final String MODEL_ATTRIBUTE = "notifications";
    @Serial
    private static final long serialVersionUID = 1L;
    private String id = "notification-" + UUID.randomUUID();
    private String title;
    private String text;
    private String style;

    public Notification() {
    }

    public Notification(String title, String text, String style) {
        this.title = title;
        this.text = text;
        this.style = style;
    }

    public static void pushToModel(String title, String text, Style style, Model model) {
        Notification notification = new Notification(title, text, style.name());
        Object attribute = model.getAttribute(MODEL_ATTRIBUTE);
        if (attribute instanceof Notification) {
            List<Notification> objects = new ArrayList<>();
            objects.add((Notification) attribute);
            objects.add(notification);
            model.addAttribute(MODEL_ATTRIBUTE, objects);
        } else if (attribute instanceof Collection<?>) {
            List<Object> objects = new ArrayList<>((Collection<?>) attribute);
            objects.add(notification);
            model.addAttribute(MODEL_ATTRIBUTE, objects);
        } else {
            model.addAttribute(MODEL_ATTRIBUTE, Collections.singletonList(notification));
        }
    }

    public static void pushToRedirect(String title, String text, Style style, RedirectAttributes redirectAttributes) {
        Notification notification = new Notification(title, text, style.name());
        Map<String, ?> flashAttributes = redirectAttributes.getFlashAttributes();
        Object attribute = flashAttributes.get(MODEL_ATTRIBUTE);
        if (attribute instanceof Notification) {
            List<Notification> objects = new ArrayList<>();
            objects.add((Notification) attribute);
            objects.add(notification);
            redirectAttributes.addFlashAttribute(MODEL_ATTRIBUTE, objects);
        } else if (attribute instanceof Collection<?>) {
            List<Object> objects = new ArrayList<>((Collection<?>) attribute);
            objects.add(notification);
            redirectAttributes.addFlashAttribute(MODEL_ATTRIBUTE, objects);
        } else {
            redirectAttributes.addFlashAttribute(MODEL_ATTRIBUTE, Collections.singletonList(notification));
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "title='" + title + '\'' +
                ", text='" + text + '\'' +
                ", style='" + style + '\'' +
                '}';
    }

    public enum Style {
        primary,
        success,
        danger,
        light,
        info
    }
}
