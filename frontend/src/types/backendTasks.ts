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
  createdAt: string = "";

  @Expose()
  name: string = "";

  @Expose()
  status: TaskStatus = TaskStatus.Ready;

  isRunning() : boolean {
    switch (this.status) {
      case TaskStatus.Ready:
      case TaskStatus.Running:
        return true;
    }
    return false;
  }
}

export function imageHasRunningTask(imageId: number | undefined, tasks : BackendTaskPayload[] | undefined) {
  if(!imageId) return false;
  if(tasks) {
    for (const task of tasks) {
      if(task.isRunning()) {
        if(task.imageIds.includes(imageId)) {
          return true
        }
      }
    }
    return false
  }
  else {
    return false
  }
}
