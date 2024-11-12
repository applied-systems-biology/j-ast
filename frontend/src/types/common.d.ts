/**
 * Message that contains basic infos about a project
 * Shared with the backend
 */
export interface ProjectInfoMessage {
  id: number;
  name: string;
  owner: string;
}

export interface ImageInfoMessage {
  id: number,
  projectId: number,
  fileName: string,
  owner: string,
  experiment: string,
  sample: string,
  timePoint: string,
  thumbnailData: string
}

/**
 * Sent to the backend to create a project
 */
export interface CreateEditProjectRequest {
  name: string;
}
