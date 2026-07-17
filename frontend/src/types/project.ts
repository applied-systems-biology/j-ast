/*
 * Copyright (c) 2026.
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

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
