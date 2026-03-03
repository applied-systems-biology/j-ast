import { Expose } from 'class-transformer';
import { ViewMode } from 'src/types/view';

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

  @Expose()
  viewMode: ViewMode = ViewMode.Timeline;

  @Expose()
  createdAt: string = '';

  @Expose()
  updatedAt: string = '';
}

/**
 * Sent to the backend to create a project
 */
export class CreateEditProjectRequest {
  @Expose()
  name: string = "";

  @Expose()
  viewMode: ViewMode = ViewMode.Timeline;
}

/**
 * Used by the create project dialog. Has also
 */
export class CreateProjectRequest {

  @Expose()
  name: string = "";

  @Expose()
  viewMode: ViewMode = ViewMode.Timeline;

  @Expose()
  projectArchiveFile: File | null = null;
}