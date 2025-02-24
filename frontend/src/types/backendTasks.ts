import { Expose, instanceToPlain } from 'class-transformer';
import { ImagePayload, imageSupportsMaskAnnotation } from "src/types/image";
import { Dialog } from 'quasar';
import BackendTaskSetupDialog from 'components/backendProcessors/BackendTaskSetupDialog.vue';
import { ProjectImagesPayload } from 'src/types/projectImages';
import { api } from 'boot/axios';
import {
  sendFailureNotification,
  sendInfoNotification, sendSuccessNotification,
} from 'src/types/notification';
import { AssayType } from 'src/types/assayType';
import {showLoadingWithTimeout} from "src/types/common";
import { ViewMode } from 'src/types/view';

export enum TaskStatus {
  Ready = 'Ready',
  Running = 'Running',
  Successful = 'Successful',
  Failed = 'Failed',
}

export enum BackendTaskWorkloadMode {
  Single= "Single",
  FullRow = "FullRow",
  FullColumn = "FullColumn",
}

export enum BackendTaskWorkloadDataSlotType {
  ImageMaskAnnotation= "ImageMaskAnnotation",
  Metadata = "Metadata",
}

export  enum BackendTaskWorkloadParameterSlotType {
  String = "String",
  Number = "Number",
  Boolean = "Boolean",
}

export enum BackendTaskWorkloadDataSlotValidationMode {
  Always = "Always",
  Optional = "Optional",
  OncePerRow = "OncePerRow",
}

export enum BackendTaskWorkloadDataSlotValidationResult {
  Ok = "Ok",
  MandatoryMissing = "MandatoryMissing",
  OptionalMissing = "OptionalMissing",
}

export class BackendTaskWorkloadDataSlot {
  @Expose()
  type: BackendTaskWorkloadDataSlotType = BackendTaskWorkloadDataSlotType.ImageMaskAnnotation;

  @Expose()
  validationMode: BackendTaskWorkloadDataSlotValidationMode = BackendTaskWorkloadDataSlotValidationMode.Always;

  @Expose()
  name: string = "";
}

export class BackendTaskParameterPayload {
  @Expose()
  type: BackendTaskWorkloadParameterSlotType = BackendTaskWorkloadParameterSlotType.String;

  @Expose()
  id: string = "";

  @Expose()
  label: string = "";

  @Expose()
  description: string = "";

  @Expose()
  value: any = null
}

export class BackendTaskTypePayload {
  @Expose()
  taskId: string = "";

  @Expose()
  name: string = "";

  @Expose()
  description: string = "";

  @Expose()
  shortDescription: string = "";

  @Expose()
  category: string = "";

  @Expose()
  workloadMode: BackendTaskWorkloadMode = BackendTaskWorkloadMode.Single;

  @Expose()
  inputs: Array<BackendTaskWorkloadDataSlot> = [];

  @Expose()
  outputs: Array<BackendTaskWorkloadDataSlot> = [];

  @Expose()
  assayTypeRestriction: AssayType = AssayType.Unknown;

  @Expose()
  parameters: BackendTaskParameterPayload[] = [];

  @Expose()
  outputsResult: boolean = false;

  @Expose()
  viewModeRestriction: ViewMode | null = null;
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
  parameters: BackendTaskParameterPayload[] = [];

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

export interface ProgressInfo {
  current: number;
  total: number;
}

const iterationPattern = /Iteration (\d+)\/(\d+)/;
const iterationPattern2 = /\[(\d+)\/(\d+)]/;

export function progressInfoIsIndeterminate(progressInfo: ProgressInfo | null): boolean {
  if(progressInfo) {
    return progressInfo.current == progressInfo.total
  }
  return true;
}

export function progressInfoValue(progressInfo: ProgressInfo | null): number {
  if(progressInfo && progressInfo.total > 0) {
    return progressInfo.current / progressInfo.total;
  }
  return 0
}

export function extractProgressInfoFromLog(text: string): ProgressInfo | null {
  const match = text.match(iterationPattern);
  if (match) {
    return { current:  parseInt(match[1], 10), total: parseInt(match[2], 10)}
  }
  else {
    const match2 = text.match(iterationPattern2);
    if(match2) {
      return { current:  parseInt(match2[1], 10), total: parseInt(match2[2], 10)}
    }
  }
  return null;
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

export function imageSupportsBackendInputSlot(image: ImagePayload, slot: BackendTaskWorkloadDataSlot) : boolean {
  if(slot.type == BackendTaskWorkloadDataSlotType.ImageMaskAnnotation) {
    return imageSupportsMaskAnnotation(image, slot.name);
  }
  else {
    return false;
  }
}

export function validateImageBackendInputSlot(image: ImagePayload, otherImages: ImagePayload[], slot: BackendTaskWorkloadDataSlot) : BackendTaskWorkloadDataSlotValidationResult {
  if(slot.type == BackendTaskWorkloadDataSlotType.ImageMaskAnnotation) {

    if(slot.validationMode == BackendTaskWorkloadDataSlotValidationMode.Always) {
      for(const annotation of image.maskImageAnnotations) {
        if(annotation.annotationTypeId == slot.name) {
          return annotation.version > 0 ? BackendTaskWorkloadDataSlotValidationResult.Ok : BackendTaskWorkloadDataSlotValidationResult.MandatoryMissing
        }
      }
    }
    else if(slot.validationMode == BackendTaskWorkloadDataSlotValidationMode.Optional) {
      for(const annotation of image.maskImageAnnotations) {
        if(annotation.annotationTypeId == slot.name) {
          return annotation.version > 0 ? BackendTaskWorkloadDataSlotValidationResult.Ok : BackendTaskWorkloadDataSlotValidationResult.OptionalMissing
        }
      }
    }
    else if (slot.validationMode == BackendTaskWorkloadDataSlotValidationMode.OncePerRow) {
      const targetRow = image.groupRow
      for(const otherImage of otherImages) {
        if(otherImage.groupRow == targetRow) {
          for(const annotation of otherImage.maskImageAnnotations) {
            if(annotation.annotationTypeId == slot.name && annotation.version > 0) {
              return BackendTaskWorkloadDataSlotValidationResult.Ok
            }
          }
        }
      }
    }

    return BackendTaskWorkloadDataSlotValidationResult.MandatoryMissing
  }
  else {
    return BackendTaskWorkloadDataSlotValidationResult.MandatoryMissing
  }
}

export function doBackendTask(
  images: ImagePayload[],
  projectId: number,
  tool: BackendTaskTypePayload,
  projectImages: ProjectImagesPayload
) {

  console.log(images)

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

  // Filter out by assay type
  if(tool.assayTypeRestriction != AssayType.Unknown) {
    images = images.filter(img => img.assayType == tool.assayTypeRestriction)
    sendInfoNotification(`This tool only works for ${tool.assayTypeRestriction}.`)
  }

  if(images.length == 0) {
    sendFailureNotification("Tool not applicable to any of the selected images. (Did are the correct assay types assigned and are the images sorted?)")
    return
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
      showLoadingWithTimeout(3000, "Sending request to the server ...")
      api
        .post(`/task/new`, instanceToPlain(payload))
        .then(() => {
          sendSuccessNotification("Successfully sent task to the server")
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
