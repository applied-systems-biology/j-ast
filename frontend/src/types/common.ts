import { Type } from 'class-transformer';

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
  id: number = -1;
  projectId: number = -1;
  fileName: string = "";
  owner: string = "";
  experiment: string = "";
  sample: string = "";
  timePoint: string = "";
  assayType: AssayType = AssayType.Unknown;
  groupRow : number = -1;
  groupColumn : number = -1;

  /**
   * Returns true if this is a non-empty payload
   */
  isPresent() : boolean {
    return this.id < 0
  }
}

export class ProjectImagesPayloadRow {
  @Type(() => ImagePayload)
  images: ImagePayload[] = [];
}

export class ProjectImagesPayload {

  @Type(() => ImagePayload)
  imagesById : Record<string, ImagePayload> = {};

  @Type(() => ProjectImagesPayloadRow)
  groupRows: ProjectImagesPayloadRow[] = [];
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
