import {
  Type,
  plainToInstance,
  ClassConstructor,
  Expose,
  instanceToPlain,
} from 'class-transformer';
import { api } from 'boot/axios';

export function plainToInstanceStrict<T, V>(
  cls: ClassConstructor<T>,
  plain: V
): T {
  return plainToInstance(cls, plain, {
    excludeExtraneousValues: true,
    exposeUnsetFields: false,
  });
}

/**
 * Message that contains basic infos about a project
 * Shared with the backend
 */
export class ProjectMetadataPayload {
  id: number = -1;
  name: string = '';
  owner: string = '';
}

export enum AssayType {
  DDA = 'DDA',
  ETest = 'ETest',
  Unknown = 'Unknown',
}

export class ImagePayload {
  @Expose()
  id: number = -1;

  @Expose()
  projectId: number = -1;

  @Expose()
  fileName: string = '';

  @Expose()
  owner: string = '';

  @Expose()
  experiment: string = '';

  @Expose()
  sample: string = '';

  @Expose()
  timePoint: string = '';

  @Expose()
  assayType: AssayType = AssayType.Unknown;

  @Expose()
  groupRow: number = -1;

  @Expose()
  groupColumn: number = -1;

  /**
   * Returns true if this is a non-empty payload
   */
  isPresent(): boolean {
    return this.id < 0;
  }

  uploadToBackend(): Promise<void> {
    return api.post(`/image/${this.id}/update`, instanceToPlain(this));
  }
}

export class ProjectImagesPayloadRow {
  @Expose()
  @Type(() => ImagePayload)
  images: ImagePayload[] = [];

  /**
   * Sorts the images according to the group column
   */
  sortImages() {
    // Sort the list
    this.images.sort((a, b) => a.groupColumn - b.groupColumn);

    // Re-assign group order
    this.images.forEach((image, index) => {
      image.groupColumn = index;
    });
  }
}

export class ProjectImagesPayload {
  @Expose()
  projectId: number = -1;

  @Expose()
  @Type(() => ImagePayload)
  imagesById: Record<string, ImagePayload> = {};

  @Expose()
  @Type(() => Number)
  imageIds: Array<number> = [];

  @Expose()
  @Type(() => ProjectImagesPayloadRow)
  groupRows: ProjectImagesPayloadRow[] = [];

  @Expose()
  @Type(() => ProjectImagesPayloadRow)
  unsortedRow: ProjectImagesPayloadRow = new ProjectImagesPayloadRow();

  /**
   * Edits the groupRows so they contain the images from imagesById instead of their own copies
   */
  fixRowReferences() {
    for (const row of this.groupRows) {
      for (let i = 0; i < row.images.length; i++) {
        const imageId = row.images[i].id;
        if (imageId.toString() in this.imagesById) {
          row.images[i] = this.imagesById[imageId.toString()]!;
        }
      }
    }
    for (let i = 0; i < this.unsortedRow.images.length; i++) {
      const imageId = this.unsortedRow.images[i].id;
      if (imageId.toString() in this.imagesById) {
        this.unsortedRow.images[i] = this.imagesById[imageId.toString()]!;
      }
    }
  }

  /**
   * Returns the image payload by ID or a dummy (id = -1)
   * @param id the ID
   */
  getImageById(id: number): ImagePayload {
    if (id.toString() in this.imagesById) {
      return this.imagesById[id.toString()]!;
    } else {
      return new ImagePayload();
    }
  }

  /**
   * Gets an image payload from a slot
   * @param rowIndex the row (can be -1 to target the unsorted row)
   * @param columnIndex the column (for unsorted this is the index in the list)
   */
  getImageBySlot(rowIndex: number, columnIndex: number): ImagePayload | null {
    if (rowIndex >= 0) {
      if (rowIndex < this.groupRows.length) {
        const row = this.groupRows[rowIndex];
        const result = row.images.find((img) => img.groupColumn == columnIndex);
        return result || null;
      } else {
        return null;
      }
    } else {
      const result = this.unsortedRow.images[columnIndex];
      return result || null;
    }
  }

  /**
   * Re-arranges a slot inside the data
   * @param sourceSlot the source slot
   * @param targetSlot the target slot
   */
  swapOrMove(
    sourceSlot: { row: number; column: number },
    targetSlot: { row: number; column: number }
  ): boolean {
    const sourceImage = this.getImageBySlot(sourceSlot.row, sourceSlot.column);
    const targetImage = this.getImageBySlot(targetSlot.row, targetSlot.column);
    const sourceIsUnsorted = sourceSlot.row < 0;
    const targetIsUnsorted = targetSlot.row < 0;
    if (!sourceImage) {
      return false;
    }
    if (!targetImage) {
      // Move to a different slot

      if (sourceIsUnsorted && targetIsUnsorted) {
        // Move image inside unsorted to the end

        const targetRow = this.unsortedRow;

        targetRow.images.splice(targetRow.images.indexOf(sourceImage), 1);
        targetRow.images.push(sourceImage);
        sourceImage.groupColumn = targetRow.images.length;

        targetRow.sortImages();

        return true;
      } else if (sourceIsUnsorted && !targetIsUnsorted) {
        // Move from unsorted into a row
        const sourceRow = this.unsortedRow;
        while (targetSlot.row > this.groupRows.length - 1) {
          this.groupRows.push(new ProjectImagesPayloadRow());
        }
        const targetRow = this.groupRows[targetSlot.row];

        sourceRow.images.splice(sourceRow.images.indexOf(sourceImage), 1);
        sourceImage.groupColumn = targetSlot.column;
        sourceImage.groupRow = targetSlot.row;
        targetRow.images.push(sourceImage);

        sourceRow.sortImages();
        // targetRow.sortImages()

        return true;
      } else if (!sourceIsUnsorted && targetIsUnsorted) {
        // Move from row into unsorted (will always be at the end)
        const sourceRow = this.groupRows[sourceSlot.row];
        const targetRow = this.unsortedRow;

        sourceRow.images.splice(sourceRow.images.indexOf(sourceImage), 1);
        sourceImage.groupColumn = targetRow.images.length;
        sourceImage.groupRow = -1;
        targetRow.images.push(sourceImage);

        // sourceRow.sortImages()
        targetRow.sortImages();

        return true;
      } else if (!sourceIsUnsorted && !targetIsUnsorted) {
        // Move
        const sourceRow = this.groupRows[sourceSlot.row];
        while (targetSlot.row > this.groupRows.length - 1) {
          this.groupRows.push(new ProjectImagesPayloadRow());
        }
        const targetRow = this.groupRows[targetSlot.row];

        sourceRow.images.splice(sourceRow.images.indexOf(sourceImage), 1);
        sourceImage.groupColumn = targetSlot.column;
        sourceImage.groupRow = targetSlot.row;
        targetRow.images.push(sourceImage);

        return true;
      }
    } else {
      // Swap with an existing image
      if (sourceIsUnsorted && targetIsUnsorted) {
        // Swap within the unsorted row

        const targetRow = this.unsortedRow;
        const sourceIndex = targetRow.images.indexOf(sourceImage);
        const targetIndex = targetRow.images.indexOf(targetImage);

        targetRow.images[sourceIndex] = targetImage;
        targetRow.images[targetIndex] = sourceImage;

        sourceImage.groupColumn = targetSlot.column;
        targetImage.groupColumn = sourceSlot.column;
      } else if (sourceIsUnsorted && !targetIsUnsorted) {
        // Swap from source into a row

        const sourceRow = this.unsortedRow;
        const targetRow = this.groupRows[targetSlot.row];
        const sourceIndex = sourceRow.images.indexOf(sourceImage);
        const targetIndex = targetRow.images.indexOf(targetImage);
        sourceRow.images[sourceIndex] = targetImage;
        targetRow.images[targetIndex] = sourceImage;

        sourceImage.groupColumn = targetSlot.column;
        sourceImage.groupRow = targetSlot.row;
        targetImage.groupColumn = sourceSlot.column;
        targetImage.groupRow = sourceSlot.row;
      } else if (!sourceIsUnsorted && targetIsUnsorted) {
        // Swap from row into unsorted

        const sourceRow = this.groupRows[sourceSlot.row];
        const targetRow = this.unsortedRow;
        const sourceIndex = sourceRow.images.indexOf(sourceImage);
        const targetIndex = targetRow.images.indexOf(targetImage);
        sourceRow.images[sourceIndex] = targetImage;
        targetRow.images[targetIndex] = sourceImage;

        sourceImage.groupColumn = targetSlot.column;
        sourceImage.groupRow = targetSlot.row;
        targetImage.groupColumn = sourceSlot.column;
        targetImage.groupRow = sourceSlot.row;
      } else if (!sourceIsUnsorted && !targetIsUnsorted) {
        // Swap
        const sourceRow = this.groupRows[sourceSlot.row];
        const targetRow = this.groupRows[targetSlot.row];
        const sourceIndex = sourceRow.images.indexOf(sourceImage);
        const targetIndex = targetRow.images.indexOf(targetImage);
        sourceRow.images[sourceIndex] = targetImage;
        targetRow.images[targetIndex] = sourceImage;

        sourceImage.groupColumn = targetSlot.column;
        sourceImage.groupRow = targetSlot.row;
        targetImage.groupColumn = sourceSlot.column;
        targetImage.groupRow = sourceSlot.row;

        return true;
      }
    }

    return false;
  }

  /**
   * Uploads the current object to the backend
   */
  uploadToBackend(): Promise<void> {
    return api.post(
      `/project/${this.projectId}/update-images`,
      instanceToPlain(this)
    );
  }

  /**
   * Returns the largest group column value (or -1)
   */
  maxColumn() {
    return Math.max(
      ...this.groupRows.map((x) =>
        Math.max(...x.images.map((y) => y.groupColumn), -1)
      ),
      -1
    );
  }

  getNumImages() {
    return this.imageIds.length;
  }
}

export interface EditImageRequest {
  id: number;
  fileName: string;
  experiment: string;
  sample: string;
  timePoint: string;
  groupRow: number;
  groupColumn: number;
  assayType: AssayType;
}

/**
 * Sent to the backend to create a project
 */
export interface CreateEditProjectRequest {
  name: string;
}
