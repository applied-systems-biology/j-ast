import {
  ClassConstructor,
  Expose,
  instanceToPlain,
  plainToInstance,
  Type,
} from 'class-transformer';
import { api } from 'boot/axios';
import { Ref } from 'vue';

export function renderMaskAnnotationId(image: ImagePayload, id: string) {
  switch (id) {
    case 'strip-disk':
      switch (image.assayType) {
        case AssayType.DDA:
          return 'DDA disk';
        case AssayType.ETest:
          return 'ETest strip';
      }
      break;
    case 'zoi-shape':
      return 'ETest ZOI shape';
    case 'plate':
      return 'Plate';
  }
  return id;
}

export function plainToInstanceStrict<T, V>(
  cls: ClassConstructor<T>,
  plain: V
): T {
  return plainToInstance(cls, plain, {
    excludeExtraneousValues: true,
    exposeUnsetFields: false,
  });
}

export function loadPayloadInstanceFromApi<T>(
  url: string,
  type: ClassConstructor<T>,
  target: Ref<T>
): Promise<T> {
  return new Promise<T>(async (resolve, reject) => {
    api
      .get(url)
      .then((response) => {
        target.value = plainToInstance(type, response.data);
        resolve(target.value);
      })
      .catch(reject);
  });
}

export function loadDataStringFromApi(url: string) {
  return new Promise<string>((resolve) => {
    api.get(url, { responseType: 'blob' }).then((response) => {
      resolve(URL.createObjectURL(response.data));
    });
  });
}

export function loadImageElementFromDataString(
  data: Blob | MediaSource
): Promise<HTMLImageElement> {
  return new Promise<HTMLImageElement>((resolve) => {
    const backgroundImageURL = URL.createObjectURL(data);
    const imageObj = new Image();
    imageObj.src = backgroundImageURL;
    imageObj.onload = () => {
      resolve(imageObj);
    };
  });
}

export function downloadDataString(
  dataString: string,
  fileName: string = 'image.png'
) {
  const aDownloadLink = document.createElement('a');
  aDownloadLink.download = fileName;
  aDownloadLink.href = dataString;
  aDownloadLink.click();
}

export function downloadFromApi(
  url: string,
  fileName: string = 'image.png'
): Promise<void> {
  return api.get(url, { responseType: 'blob' }).then((response) => {
    const objectURL = URL.createObjectURL(response.data);
    downloadDataString(objectURL, fileName);
    URL.revokeObjectURL(objectURL);
  });
}

export function removeExtensionIfPresent(
  fileName: string,
  extensions: string[] = [
    '.png',
    '.bmp',
    '.jpg',
    '.jpeg',
    '.tif',
    '.tiff',
    '.zip',
    '.jip',
  ]
): string {
  for (const extension of extensions) {
    if (fileName.toLowerCase().endsWith(extension.toLowerCase())) {
      fileName = fileName.substring(0, fileName.length - extension.length);
    }
  }
  return fileName;
}

export function ensureExtension(
  fileName: string,
  extensions: string[] = [
    '.png',
    '.bmp',
    '.jpg',
    '.jpeg',
    '.tif',
    '.tiff',
    '.zip',
    '.jip',
  ]
) {
  for (const extension of extensions) {
    if (fileName.toLowerCase().endsWith(extension.toLowerCase())) {
      return fileName;
    }
  }
  return fileName + extensions[0];
}

export function uploadImage(url: string, dataUri: string): Promise<void> {
  // Extract the base64 data from the data URI
  const base64Data = dataUri.split(',')[1];
  const byteCharacters = atob(base64Data);
  const byteNumbers = new Array(byteCharacters.length);

  for (let i = 0; i < byteCharacters.length; i++) {
    byteNumbers[i] = byteCharacters.charCodeAt(i);
  }

  const byteArray = new Uint8Array(byteNumbers);
  const blob = new Blob([byteArray], { type: 'image/png' });

  // Create FormData and append the image
  const formData = new FormData();
  formData.append('file', blob, 'image.png');

  // Perform the Axios POST request
  return api.post(url, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
}

/**
 * Message that contains basic infos about a project
 * Shared with the backend
 */
export class ProjectMetadataPayload {
  @Expose()
  id: number = -1;

  @Expose()
  name: string = '';

  @Expose()
  owner: string = '';
}

export class MaskImageAnnotationPayload {
  @Expose()
  id: number = -1;

  @Expose()
  version: number = -1;

  @Expose()
  imageId: number = -1;

  @Expose()
  projectId: number = -1;

  @Expose()
  annotationTypeId: string = '';
}

export enum AssayType {
  DDA = 'DDA',
  ETest = 'ETest',
  Unknown = 'Unknown',
}

export function parseAssayType(str: string): AssayType {
  if (str) {
    str = str.toLowerCase();
    if (str == 'dda') {
      return AssayType.DDA;
    } else if (str == 'etest') {
      return AssayType.ETest;
    } else if (str.startsWith('e') && str.endsWith('test')) {
      return AssayType.ETest;
    } else {
      return AssayType.Unknown;
    }
  } else {
    return AssayType.Unknown;
  }
}

export interface Badge {
  text: string;
  color: string;
  icon: string;
  type: string;
}

function createExperimentBadge(value: string): Badge {
  return {
    text: value,
    icon: 'fa-solid fa-vial-virus',
    color: '#62a0ea',
    type: 'Experiment',
  };
}

function createSampleBadge(value: string): Badge {
  return {
    text: value,
    icon: 'fa-solid fa-flask',
    color: '#33d17a',
    type: 'Sample',
  };
}

function createTimePointBadge(value: string): Badge {
  return {
    text: value,
    icon: 'fa-solid fa-clock',
    color: '#e5a50a',
    type: 'Time point',
  };
}

function createAssayTypeBadge(value: string): Badge {
  return {
    text: value,
    icon: 'fa-solid fa-gear',
    color: '#9141ac',
    type: 'Assay type',
  };
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

  @Expose()
  maskImageAnnotations: MaskImageAnnotationPayload[] = [];

  /**
   * Returns true if this is a non-empty payload
   */
  isPresent(): boolean {
    return this.id < 0;
  }

  uploadToBackend(): Promise<void> {
    return api.post(`/image/${this.id}/update`, instanceToPlain(this));
  }

  getMetadataAsBadges(): Array<Badge> {
    const result: Array<Badge> = [];
    if (this.experiment) {
      result.push(createExperimentBadge(this.experiment));
    }
    if (this.sample) {
      result.push(createSampleBadge(this.sample));
    }
    if (this.timePoint) {
      result.push(createTimePointBadge(this.timePoint));
    }
    if (this.assayType && this.assayType != AssayType.Unknown) {
      result.push(createAssayTypeBadge(this.assayType));
    }
    return result;
  }

  getAnnotationsAsBadges(): Array<Badge> {
    const result: Array<Badge> = [];
    for (const annotation of this.maskImageAnnotations) {
      if (annotation.version > 0) {
        result.push({
          text: annotation.annotationTypeId,
          icon: 'fa-solid fa-tag',
          color: '#164089',
          type: 'mask-annotation/' + annotation.annotationTypeId,
        });
      }
    }
    return result;
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

  removeById(id: number) {
    this.images = this.images.filter((image) => image.id !== id);
  }

  removeDuplicates() {
    const byId: Record<string, ImagePayload> = {};
    for (const image of this.images) {
      byId[image.id.toString()] = image;
    }
    this.images = Array.from(Object.values(byId));
  }

  getImageByColumn(columnIndex: number): ImagePayload | null {
    return this.images.find((img) => img.groupColumn == columnIndex) || null
  }

  /**
   * Row metadata (experiment, sample, assay type) as record.
   * Only contains a k-v pair if it is unique
   */
  getUniqueRowMetadata() : Record<string, string> {
    const result : Record<string, string> = {};
    const allExperiments = new Set<string>();
    const allSamples = new Set<string>();
    const allAssayTypes = new Set<string>();
    for (const image of this.images) {
      if (image.experiment) {
        allExperiments.add(image.experiment);
      }
      if (image.sample) {
        allSamples.add(image.sample);
      }
      if (image.assayType && image.assayType != AssayType.Unknown) {
        allAssayTypes.add(image.assayType);
      }
    }
    if(allExperiments.size == 1) {
      result["experiment"] = [...allExperiments][0]
    }
    if(allSamples.size == 1) {
      result["sample"] = [...allSamples][0]
    }
    if(allAssayTypes.size == 1) {
      result["assayType"] = [...allAssayTypes][0]
    }
    return result;
  }

  /**
   * Row metadata (experiment, sample, assay type) as badges
   */
  getRowMetadataAsBadges(): Array<Badge> {
    const result: Array<Badge> = [];
    const allExperiments = new Set<string>();
    const allSamples = new Set<string>();
    const allAssayTypes = new Set<string>();
    for (const image of this.images) {
      if (image.experiment) {
        allExperiments.add(image.experiment);
      }
      if (image.sample) {
        allSamples.add(image.sample);
      }
      if (image.assayType && image.assayType != AssayType.Unknown) {
        allAssayTypes.add(image.assayType);
      }
    }
    for (const value of allExperiments) {
      result.push(createExperimentBadge(value));
    }
    for (const value of allSamples) {
      result.push(createSampleBadge(value));
    }
    for (const value of allAssayTypes) {
      result.push(createAssayTypeBadge(value));
    }
    if (allExperiments.size == 0) {
      result.push({
        text: 'N/A',
        icon: 'fa-solid fa-vial-virus',
        color: '#c0bfbc',
        type: 'NAExperiment',
      });
    }
    if (allSamples.size == 0) {
      result.push({
        text: 'N/A',
        icon: 'fa-solid fa-flask',
        color: '#c0bfbc',
        type: 'NASample',
      });
    }
    if (allAssayTypes.size == 0) {
      result.push({
        text: 'N/A',
        icon: 'fa-solid fa-gear',
        color: '#c0bfbc',
        type: 'NAAssayType',
      });
    }
    return result;
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
    console.log(sourceIsUnsorted, targetIsUnsorted);
    if (!targetImage) {
      // Move to a different slot

      if (sourceIsUnsorted && targetIsUnsorted) {
        // Move image inside unsorted to the end

        const targetRow = this.unsortedRow;

        targetRow.removeById(sourceImage.id);
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

        sourceRow.removeById(sourceImage.id);
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

        sourceRow.removeById(sourceImage.id);
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

        sourceRow.removeById(sourceImage.id);
        sourceImage.groupColumn = targetSlot.column;
        sourceImage.groupRow = targetSlot.row;
        targetRow.images.push(sourceImage);

        targetRow.removeDuplicates();

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

  getUniqueColumnMetadata(column: number) {
    const result : Record<string, string> = {}

    const allTimePoints = new Set<string>();
    for (const row of this.groupRows) {
      for (const image of row.images) {
        if (image.groupColumn == column && image.timePoint) {
          allTimePoints.add(image.timePoint);
        }
      }
    }

    if(allTimePoints.size == 1) {
      result["timePoint"] = [...allTimePoints][0]
    }

    return result;
  }

  getColumnMetadataAsBadges(column: number): Array<Badge> {
    const result: Array<Badge> = [];
    const allTimePoints = new Set<string>();
    for (const row of this.groupRows) {
      for (const image of row.images) {
        if (image.groupColumn == column && image.timePoint) {
          allTimePoints.add(image.timePoint);
        }
      }
    }
    for (const value of allTimePoints) {
      result.push(createTimePointBadge(value));
    }
    if (allTimePoints.size == 0) {
      result.push({
        text: 'N/A',
        icon: 'fa-solid fa-clock',
        color: '#c0bfbc',
        type: 'NATimePoint',
      });
    }
    return result;
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
