package org.hkijena.jast.payloads;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.entities.Project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Payload that gives a structured view on the images of a project
 */
public class ProjectImagesPayload {

    private Map<Long, ImagePayload> imagesById = new HashMap<>();
    private List<Row> groupRows = new ArrayList<>();

    public ProjectImagesPayload() {

    }

    public ProjectImagesPayload(Project project) {
        int maxRow = -1;
        for (Image image : project.getImages()) {
            imagesById.put(image.getId(), ImagePayload.create(image));
            maxRow = Math.max(maxRow, image.getGroupRow());
        }

    }

    @JsonGetter("groupRows")
    public List<Row> getGroupRows() {
        return groupRows;
    }

    @JsonSetter("groupRows")
    public void setGroupRows(List<Row> groupRows) {
        this.groupRows = groupRows;
    }

    @JsonGetter("imagesById")
    public Map<Long, ImagePayload> getImagesById() {
        return imagesById;
    }

    @JsonSetter("imagesById")
    public void setImagesById(Map<Long, ImagePayload> imagesById) {
        this.imagesById = imagesById;
    }

    public static class Row {
        private List<ImagePayload> images = new ArrayList<>();

        @JsonGetter("images")
        public List<ImagePayload> getImages() {
            return images;
        }

        @JsonSetter("images")
        public void setImages(List<ImagePayload> images) {
            this.images = images;
        }
    }
}
