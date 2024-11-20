import { Expose } from 'class-transformer';

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

/**
 * Sent to the backend to create a project
 */
export interface CreateEditProjectRequest {
  name: string;
}
