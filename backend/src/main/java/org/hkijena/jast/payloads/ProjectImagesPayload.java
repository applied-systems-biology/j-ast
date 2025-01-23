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

    private long projectId;
    private Map<Long, ImagePayload> imagesById = new HashMap<>();
    private List<Long> imageIds = new ArrayList<>();
    private Row unsortedRow = new Row();
    private List<Row> groupRows = new ArrayList<>();

    public ProjectImagesPayload() {

    }

    public ProjectImagesPayload(Project project) {
        this.projectId = project.getId();
        int numRows = 0;
        for (Image image : project.getImages()) {
            imageIds.add(image.getId());
            imagesById.put(image.getId(), new ImagePayload(image));
            numRows = Math.max(numRows, image.getGroupRow() + 1);

            // Sort unsorted images
            if (image.getGroupRow() < 0 || image.getGroupColumn() < 0) {
                unsortedRow.images.add(new ImagePayload(image));
            }
        }
        unsortedRow.images.sort(Comparator.comparing(ImagePayload::getGroupColumn));

        // We assign a group column based on the order
        List<ImagePayload> images = unsortedRow.images;
        for (int i = 0; i < images.size(); i++) {
            ImagePayload image = images.get(i);
            image.setGroupColumn(i);
        }

        // Handle the sorted rows
        if (numRows > 0) {
            for (int i = 0; i < numRows; i++) {
                Row row = new Row();
                for (Image image : project.getImages()) {
                    if (image.getGroupRow() == i && image.getGroupColumn() >= 0) {
                        row.images.add(new ImagePayload(image));
                    }
                }
                row.images.sort(Comparator.comparing(ImagePayload::getGroupColumn));
                groupRows.add(row);
            }
        }
    }

    @JsonGetter("projectId")
    public long getProjectId() {
        return projectId;
    }

    @JsonSetter("projectId")
    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    @JsonGetter("imageIds")
    public List<Long> getImageIds() {
        return imageIds;
    }

    @JsonSetter("imageIds")
    public void setImageIds(List<Long> imageIds) {
        this.imageIds = imageIds;
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
