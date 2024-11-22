import {Expose, instanceToPlain} from 'class-transformer';
import {ImagePayload} from "src/types/image";
import {Dialog} from "quasar";
import BackendTaskSetupDialog from "components/backendProcessors/BackendTaskSetupDialog.vue";
import {ProjectImagesPayload} from "src/types/projectImages";
import {api} from "boot/axios";
import {sendFailureNotification, sendInfoNotification} from "src/types/notification";

export enum TaskStatus {
  Ready = "Ready",
  Running = "Running",
  Successful = "Successful",
  Failed = "Failed"
}

export enum BackendTaskWorkloadMode {
  Single= "Single",
  FullRow = "FullRow",
  FullColumn = "FullColumn",
}

export enum BackendTaskWorkloadDataSlotType {
  ImageMaskAnnotation= "ImageMaskAnnotation",
}

export class BackendTaskWorkloadDataSlot {
  @Expose()
  type: BackendTaskWorkloadDataSlotType = BackendTaskWorkloadDataSlotType.ImageMaskAnnotation;

  @Expose()
  name: string = "";
}

export class BackendTaskTypePayload {
  @Expose()
  taskId: string = "";

  @Expose()
  name: string = "";

  @Expose()
  description: string = "";

  @Expose()
  workloadMode: BackendTaskWorkloadMode = BackendTaskWorkloadMode.Single;

  @Expose()
  inputs: Array<BackendTaskWorkloadDataSlot> = [];

  @Expose()
  outputs: Array<BackendTaskWorkloadDataSlot> = [];
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

  isRunning(): boolean {
    switch (this.status) {
      case TaskStatus.Ready:
      case TaskStatus.Running:
        return true;
    }
    return false;
  }
}

export function imageHasRunningTask(imageId: number | undefined, tasks: BackendTaskPayload[] | undefined) {
  if (!imageId) return false;
  if (tasks) {
    for (const task of tasks) {
      if (task.isRunning()) {
        if (task.imageIds.includes(imageId)) {
          return true
        }
      }
    }
    return false
  } else {
    return false
  }
}

export function doBackendTask(
  images: ImagePayload[],
  projectId: number,
  tool: BackendTaskTypePayload,
  projectImages: ProjectImagesPayload
) {

  // Do modifications to the list of images based on the workload type
  if(tool.workloadMode == BackendTaskWorkloadMode.FullRow) {
    const collectedRows = new Set<number>()
    for(const image of images) {
      if(image.groupRow >= 0) {
        collectedRows.add(image.groupRow)
      }
    }
    images = []
    for (let row = 0; row < projectImages.groupRows.length; row++) {
      if(collectedRows.has(row)) {
        images.push(...projectImages.groupRows[row].images)
      }
    }
  }
  else if(tool.workloadMode == BackendTaskWorkloadMode.FullColumn) {
    const collectedColumns = new Set<number>()
    for(const image of images) {
      if(image.groupRow >= 0 && image.groupColumn >= 0) {
        collectedColumns.add(image.groupColumn)
      }
    }
    images = []
    for(const row of projectImages.groupRows) {
      for(const image of row.images) {
        if(collectedColumns.has(image.groupColumn)) {
          images.push(image)
        }
      }
    }
  }

  Dialog.create({
    component: BackendTaskSetupDialog,
    componentProps: {
      images: images,
      projectId: projectId,
      taskType: tool,
      persistent: true,
    },
  })
    .onOk((payload: BackendTaskPayload) => {
      api
        .post(`/task/new`, instanceToPlain(payload))
        .then(() => {
          sendInfoNotification("Sent task request to the server")
        })
        .catch(() => {
          sendFailureNotification('Unable to start task');
        });
    })
    .onCancel(() => {
    })
    .onDismiss(() => {
    });
}
