import { Expose } from 'class-transformer';

export class BackendTaskInfoPayload {
  @Expose()
  taskId: string;

  @Expose()
  name: string;

  @Expose()
  description: string;

  constructor(taskId: string, name: string, description: string) {
    this.taskId = taskId;
    this.name = name;
    this.description = description;
  }
}

export class BackendTaskPayload {
  @Expose()
  id: number = -1;

  @Expose()
  imageIds: number[] = [];

  @Expose()
  taskId: string;

  @Expose()
  projectId: number;

  constructor(id: number = -1, imageIds: number[] = [], taskId: string, projectId: number) {
    this.id = id;
    this.imageIds = imageIds;
    this.taskId = taskId;
    this.projectId = projectId;
  }
}
