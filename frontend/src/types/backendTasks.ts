import { Expose } from 'class-transformer';

export enum TaskStatus {
  Ready= "Ready",
  Running = "Running",
  Successful = "Successful",
  Failed = "Failed"
}

export class BackendTaskTypePayload {
  @Expose()
  taskId: string = "";

  @Expose()
  name: string = "";

  @Expose()
  description: string = "";
}

export class BackendTaskPayload {
  @Expose()
  id: number = -1;

  @Expose()
  imageIds: number[] = [];

  @Expose()
  taskId: string = "";

  @Expose()
  projectId: number = -1;

  @Expose()
  status: TaskStatus = TaskStatus.Ready;
}
