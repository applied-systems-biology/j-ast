/**
 * Message that contains basic infos about a project
 * Shared with the backend
 */
export interface ProjectMetadataPayload {
  id: number;
  name: string;
  owner: string;
}

export enum AssayType {
  DDA = "DDA",
  ETest = "ETest",
  Unknown = "Unknown",
}

export interface ImagePayload {
  id: number,
  projectId: number,
  fileName: string,
  owner: string,
  experiment: string,
  sample: string,
  timePoint: string,
  assayType: AssayType,
  groupRow : number,
  groupColumn : number
}

export interface ProjectImagesPayloadRow {
  images: ImagePayload[];
}

export interface ProjectImagesPayload {
  imagesById : Map<number, ImagePayload>,
  groupRows: ProjectImagesPayloadRow[],
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
