package org.hkijena.jast.payloads;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jast.model.AssayType;
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
    private RowPayload unsortedRow = new RowPayload();
    private List<RowPayload> groupRows = new ArrayList<>();

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
                RowPayload row = new RowPayload();
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
    public RowPayload getUnsortedRow() {
        return unsortedRow;
    }

    @JsonSetter("unsortedRow")
    public void setUnsortedRow(RowPayload unsortedRow) {
        this.unsortedRow = unsortedRow;
    }

    @JsonGetter("groupRows")
    public List<RowPayload> getGroupRows() {
        return groupRows;
    }

    @JsonSetter("groupRows")
    public void setGroupRows(List<RowPayload> groupRows) {
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

    public static class RowPayload {
        private List<ImagePayload> images = new ArrayList<>();

        @JsonGetter("images")
        public List<ImagePayload> getImages() {
            return images;
        }

        @JsonSetter("images")
        public void setImages(List<ImagePayload> images) {
            this.images = images;
        }

        public ImagePayload getImageByColumn(int columnIndex) {
            return this.getImages().stream()
                    .filter(img -> img.getGroupColumn() == columnIndex)
                    .findFirst()
                    .orElse(null);
        }

        /**
         * Sorts the images according to the group column
         */
        public void sortImages() {
            this.getImages().sort(Comparator.comparingInt(ImagePayload::getGroupColumn));
            for (int i = 0; i < this.getImages().size(); i++) {
                this.getImages().get(i).setGroupColumn(i);
            }
        }

        /**
         * Removes duplicate images based on their ID
         */
        public void removeDuplicates() {
            Map<Long, ImagePayload> byId = new HashMap<>();
            for (ImagePayload image : this.getImages()) {
                byId.put(image.getId(), image);
            }
            this.getImages().clear();
            this.getImages().addAll(byId.values());
        }

        /**
         * Removes an image by its ID
         */
        public void removeById(long id) {
            this.getImages().removeIf(image -> image.getId() == id);
        }

        public Map<String, String> getUniqueRowMetadata() {
            Map<String, String> result = new HashMap<>();
            Set<String> allExperiments = new HashSet<>();
            Set<String> allSamples = new HashSet<>();
            Set<String> allAssayTypes = new HashSet<>();

            for (ImagePayload image : this.getImages()) {
                if (image.getExperiment() != null) {
                    allExperiments.add(image.getExperiment());
                }
                if (image.getSample() != null) {
                    allSamples.add(image.getSample());
                }
                if (image.getAssayType() != null && !image.getAssayType().equals(AssayType.Unknown)) {
                    allAssayTypes.add(image.getAssayType().toString());
                }
            }

            if (allExperiments.size() == 1) {
                result.put("experiment", allExperiments.iterator().next());
            }
            if (allSamples.size() == 1) {
                result.put("sample", allSamples.iterator().next());
            }
            if (allAssayTypes.size() == 1) {
                result.put("assayType", allAssayTypes.iterator().next());
            }

            return result;
        }
    }

    public void autoSort(List<ImagePayload> images, List<String> timePointOrder) {
        Map<String, Integer> columnIndicesPlus1 = new HashMap<>();
        int maxColumn = maxColumn();
        int minSearchColumnIndex = 0;

        // Assign time points to column indices
        for (String requestedTimePoint : timePointOrder) {
            int newColumnIndex = maxColumn + 1;

            // Find an existing column with the same time point
            for (int columnIndex = minSearchColumnIndex; columnIndex <= maxColumn; columnIndex++) {
                Map<String, String> uniqueMetadata = getUniqueColumnMetadata(columnIndex);
                if (uniqueMetadata.containsKey("timePoint")) {
                    String columnTimePoint = uniqueMetadata.get("timePoint");
                    if (columnTimePoint.equals(requestedTimePoint)) {
                        newColumnIndex = columnIndex;
                        break;
                    }
                }
            }

            // Store column index and update search bounds
            columnIndicesPlus1.put(requestedTimePoint, newColumnIndex + 1);
            minSearchColumnIndex = newColumnIndex + 1;
            maxColumn = Math.max(maxColumn, newColumnIndex);
        }

        if (columnIndicesPlus1.isEmpty()) {
            throw new RuntimeException("Unable to find time point columns!");
        }

        int numSuccess = 0;
        int numFailed = 0;

        // Assign rows to images
        for (ImagePayload image : images) {
            if (image.getGroupRow() < 0) {
                int newRow = getGroupRows().size(); // Default to next row
                int columnIndex = columnIndicesPlus1.getOrDefault(image.getTimePoint(), 0) - 1;

                if (columnIndex < 0) {
                    numFailed++;
                    continue;
                }

                for (int rowIndex = 0; rowIndex < getGroupRows().size(); rowIndex++) {
                    RowPayload rowPayload = getGroupRows().get(rowIndex);
                    Map<String, String> uniqueMetadata = rowPayload.getUniqueRowMetadata();

                    if (Objects.equals(image.getExperiment(), uniqueMetadata.get("experiment")) &&
                            Objects.equals(image.getSample(), uniqueMetadata.get("sample")) &&
                            Objects.equals(image.getAssayType(), uniqueMetadata.get("assayType"))) {

                        if (columnIndex >= 0 && rowPayload.getImageByColumn(columnIndex) == null) {
                            newRow = rowIndex;
                            break;
                        }
                    }
                }

                int indexInUnsorted = getUnsortedRow().getImages().indexOf(image);
                if (indexInUnsorted >= 0) {
                    if (swapOrMove(new Position(image.getGroupRow(), indexInUnsorted),
                            new Position(newRow, columnIndex))) {
                        numSuccess++;
                    } else {
                        numFailed++;
                    }
                } else {
                    numFailed++;
                }
            }
        }
    }

    /**
     * Gets an image payload from a slot
     * @param rowIndex the row (can be -1 to target the unsorted row)
     * @param columnIndex the column (for unsorted this is the index in the list)
     */
    public ImagePayload getImageBySlot(int rowIndex, int columnIndex) {
        if (rowIndex >= 0) {
            if (rowIndex < this.getGroupRows().size()) {
                RowPayload row = this.getGroupRows().get(rowIndex);
                return row.getImages().stream()
                        .filter(img -> img.getGroupColumn() == columnIndex)
                        .findFirst()
                        .orElse(null);
            } else {
                return null;
            }
        } else {
            List<ImagePayload> images = this.getUnsortedRow().getImages();
            return (columnIndex >= 0 && columnIndex < images.size()) ? images.get(columnIndex) : null;
        }
    }

    public boolean swapOrMove(Position sourceSlot, Position targetSlot) {
        ImagePayload sourceImage = this.getImageBySlot(sourceSlot.row, sourceSlot.column);
        ImagePayload targetImage = this.getImageBySlot(targetSlot.row, targetSlot.column);
        boolean sourceIsUnsorted = sourceSlot.row < 0;
        boolean targetIsUnsorted = targetSlot.row < 0;

        if (sourceImage == null) {
            return false;
        }

        if (targetImage == null) {
            if (sourceIsUnsorted && targetIsUnsorted) {
                RowPayload targetRow = this.getUnsortedRow();
                targetRow.removeById(sourceImage.getId());
                targetRow.getImages().add(sourceImage);
                sourceImage.setGroupColumn(targetRow.getImages().size());
                targetRow.sortImages();
                return true;
            } else if (sourceIsUnsorted && !targetIsUnsorted) {
                RowPayload sourceRow = this.getUnsortedRow();
                while (targetSlot.row >= this.getGroupRows().size()) {
                    this.getGroupRows().add(new RowPayload());
                }
                RowPayload targetRow = this.getGroupRows().get(targetSlot.row);
                sourceRow.removeById(sourceImage.getId());
                sourceImage.setGroupColumn(targetSlot.column);
                sourceImage.setGroupRow(targetSlot.row);
                targetRow.getImages().add(sourceImage);
                sourceRow.sortImages();
                return true;
            } else if (!sourceIsUnsorted && targetIsUnsorted) {
                RowPayload sourceRow = this.getGroupRows().get(sourceSlot.row);
                RowPayload targetRow = this.getUnsortedRow();
                sourceRow.removeById(sourceImage.getId());
                sourceImage.setGroupColumn(targetRow.getImages().size());
                sourceImage.setGroupRow(-1);
                targetRow.getImages().add(sourceImage);
                targetRow.sortImages();
                return true;
            } else if (!sourceIsUnsorted && !targetIsUnsorted) {
                RowPayload sourceRow = this.getGroupRows().get(sourceSlot.row);
                while (targetSlot.row >= this.getGroupRows().size()) {
                    this.getGroupRows().add(new RowPayload());
                }
                RowPayload targetRow = this.getGroupRows().get(targetSlot.row);
                sourceRow.removeById(sourceImage.getId());
                sourceImage.setGroupColumn(targetSlot.column);
                sourceImage.setGroupRow(targetSlot.row);
                targetRow.getImages().add(sourceImage);
                targetRow.removeDuplicates();
                return true;
            }
        } else {
            if (sourceIsUnsorted && targetIsUnsorted) {
                RowPayload targetRow = this.getUnsortedRow();
                Collections.swap(targetRow.getImages(), targetRow.getImages().indexOf(sourceImage), targetRow.getImages().indexOf(targetImage));
                sourceImage.setGroupColumn(targetSlot.column);
                targetImage.setGroupColumn(sourceSlot.column);
            } else if (sourceIsUnsorted && !targetIsUnsorted) {
                RowPayload sourceRow = this.getUnsortedRow();
                RowPayload targetRow = this.getGroupRows().get(targetSlot.row);
                Collections.swap(sourceRow.getImages(), sourceRow.getImages().indexOf(sourceImage), targetRow.getImages().indexOf(targetImage));
                sourceImage.setGroupColumn(targetSlot.column);
                sourceImage.setGroupRow(targetSlot.row);
                targetImage.setGroupColumn(sourceSlot.column);
                targetImage.setGroupRow(sourceSlot.row);
            } else if (!sourceIsUnsorted && targetIsUnsorted) {
                RowPayload sourceRow = this.getGroupRows().get(sourceSlot.row);
                RowPayload targetRow = this.getUnsortedRow();
                Collections.swap(sourceRow.getImages(), sourceRow.getImages().indexOf(sourceImage), targetRow.getImages().indexOf(targetImage));
                sourceImage.setGroupColumn(targetSlot.column);
                sourceImage.setGroupRow(targetSlot.row);
                targetImage.setGroupColumn(sourceSlot.column);
                targetImage.setGroupRow(sourceSlot.row);
            } else if (!sourceIsUnsorted && !targetIsUnsorted) {
                RowPayload sourceRow = this.getGroupRows().get(sourceSlot.row);
                RowPayload targetRow = this.getGroupRows().get(targetSlot.row);
                Collections.swap(sourceRow.getImages(), sourceRow.getImages().indexOf(sourceImage), targetRow.getImages().indexOf(targetImage));
                sourceImage.setGroupColumn(targetSlot.column);
                sourceImage.setGroupRow(targetSlot.row);
                targetImage.setGroupColumn(sourceSlot.column);
                targetImage.setGroupRow(sourceSlot.row);
                return true;
            }
        }
        return false;
    }

    public Map<String, String> getUniqueColumnMetadata(int columnIndex) {
        Map<String, String> result = new HashMap<>();
        Set<String> allTimePoints = new HashSet<>();

        for (RowPayload row : this.getGroupRows()) {
            for (ImagePayload image : row.getImages()) {
                if (image.getGroupColumn() == columnIndex && image.getTimePoint() != null) {
                    allTimePoints.add(image.getTimePoint());
                }
            }
        }

        if (allTimePoints.size() == 1) {
            result.put("timePoint", allTimePoints.iterator().next());
        }

        return result;
    }

    public static class Position {
        int row, column;
        public Position(int row, int column) {
            this.row = row;
            this.column = column;
        }
    }

    public int maxColumn() {
        return this.getGroupRows().stream()
                .mapToInt(row -> row.getImages().stream()
                        .mapToInt(ImagePayload::getGroupColumn)
                        .max()
                        .orElse(-1))
                .max()
                .orElse(-1);
    }

}
