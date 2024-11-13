package org.hkijena.jast.payloads;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jast.model.entities.Image;
import org.hkijena.jast.model.entities.Project;

import java.util.*;

/**
 * Payload that gives a structured view on the images of a project
 */
public class ProjectImagesPayload {

    private Map<Long, ImagePayload> imagesById = new HashMap<>();
    private Row unsortedRow = new Row();
    private List<Row> groupRows = new ArrayList<>();

    public ProjectImagesPayload() {

    }

    public ProjectImagesPayload(Project project) {
        int maxRow = -1;
        for (Image image : project.getImages()) {
            imagesById.put(image.getId(), ImagePayload.create(image));
            maxRow = Math.max(maxRow, image.getGroupRow());

            // Sort unsorted images
            if(image.getGroupRow() < 0 || image.getGroupColumn() < 0) {
                unsortedRow.images.add(ImagePayload.create(image));
            }
        }

        if(maxRow > 0) {
            for (int i = 0; i < maxRow + 1; i++) {
                Row row = new Row();
                for (Image image : project.getImages()) {
                    if (image.getGroupRow() == i && image.getGroupColumn() >= 0) {
                        row.images.add(ImagePayload.create(image));
                    }
                }
                row.images.sort(Comparator.comparing(ImagePayload::groupColumn));
                groupRows.add(row);
            }
        }
    }

    @JsonGetter("unsortedRow")
    public Row getUnsortedRow() {
        return unsortedRow;
    }

    @JsonSetter("unsortedRow")
    public void setUnsortedRow(Row unsortedRow) {
        this.unsortedRow = unsortedRow;
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
