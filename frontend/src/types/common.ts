import { Type, plainToInstance, ClassConstructor, Expose } from 'class-transformer';

export function plainToInstanceStrict<T, V>(cls : ClassConstructor<T>, plain: V): T {
  return plainToInstance(cls, plain, { excludeExtraneousValues: true, exposeUnsetFields: false })
}

/**
 * Message that contains basic infos about a project
 * Shared with the backend
 */
export class ProjectMetadataPayload {
  id: number = -1;
  name: string = "";
  owner: string = "";
}

export enum AssayType {
  DDA = "DDA",
  ETest = "ETest",
  Unknown = "Unknown",
}

export class ImagePayload {
  @Expose()
  id: number = -1;

  @Expose()
  projectId: number = -1;

  @Expose()
  fileName: string = "";

  @Expose()
  owner: string = "";

  @Expose()
  experiment: string = "";

  @Expose()
  sample: string = "";

  @Expose()
  timePoint: string = "";

  @Expose()
  assayType: AssayType = AssayType.Unknown;

  @Expose()
  groupRow : number = -1;

  @Expose()
  groupColumn : number = -1;

  /**
   * Returns true if this is a non-empty payload
   */
  isPresent() : boolean {
    return this.id < 0
  }
}

export class ProjectImagesPayloadRow {
  @Expose()
  @Type(() => ImagePayload)
  images: ImagePayload[] = [];
}

export class ProjectImagesPayload {

  @Expose()
  @Type(() => ImagePayload)
  imagesById : Record<string, ImagePayload> = {};

  @Expose()
  @Type(() => Number)
  imageIds : Array<number> = [];

  @Expose()
  @Type(() => ProjectImagesPayloadRow)
  groupRows: ProjectImagesPayloadRow[] = [];

  @Expose()
  unsortedRow: ProjectImagesPayloadRow = new ProjectImagesPayloadRow();

  /**
   * Edits the groupRows so they contain the images from imagesById instead of their own copies
   */
  fixRowReferences() {
    for(const row of this.groupRows) {
      for (let i = 0; i < row.images.length; i++) {
        const imageId = row.images[i].id;
        if (imageId.toString() in this.imagesById) {
          row.images[i] = this.imagesById[imageId.toString()]!;
        }
      }
    }
    for (let i = 0; i < this.unsortedRow.images.length; i++) {
      const imageId =  this.unsortedRow.images[i].id;
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
    if(id.toString() in this.imagesById) {
      return this.imagesById[id.toString()]!;
    }
    else {
      return new ImagePayload()
    }
  }

  /**
   * Returns the largest group column value (or -1)
   */
  maxColumn() {
    return Math.max(...this.groupRows.map(x => Math.max(...x.images.map(y => y.groupColumn),-1)), -1);
  }

  getNumImages() {
    return this.imageIds.length
  }
}

export interface EditImageRequest {
  id: number,
  fileName: string,
  experiment: string,
  sample: string,
  timePoint: string,
  groupRow : number,
  groupColumn : number,
  assayType: AssayType
}

/**
 * Sent to the backend to create a project
 */
export interface CreateEditProjectRequest {
  name: string;
}
