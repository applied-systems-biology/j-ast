import { Expose, instanceToPlain } from 'class-transformer';
import { AssayType } from 'src/types/assayType';
import { api } from 'boot/axios';
import {
  Badge,
  createAssayTypeBadge,
  createExperimentBadge,
  createPixelSizeBadge,
  createSampleBadge,
  createTimePointBadge
} from 'src/types/badge';

export class MaskImageAnnotationPayload {
  @Expose()
  id: number = -1;

  @Expose()
  version: number = -1;

  @Expose()
  imageId: number = -1;

  @Expose()
  projectId: number = -1;

  @Expose()
  annotationTypeId: string = '';

  @Expose()
  size: number = 0;
}

export class ImagePayload {
  @Expose()
  id: number = -1;

  @Expose()
  projectId: number = -1;

  @Expose()
  fileName: string = '';

  @Expose()
  owner: string = '';

  @Expose()
  experiment: string = '';

  @Expose()
  sample: string = '';

  @Expose()
  timePoint: string = '';

  @Expose()
  assayType: AssayType = AssayType.Unknown;

  @Expose()
  mic: number = 0;

  @Expose()
  groupRow: number = -1;

  @Expose()
  groupColumn: number = -1;

  @Expose()
  version: number = -1;

  @Expose()
  pixelSizeMillimeter : number = -1;

  @Expose()
  metadata : Record<string, any> = {};

  @Expose()
  maskImageAnnotations: MaskImageAnnotationPayload[] = [];

  @Expose()
  size: number = 0;

  /**
   * Returns true if this is a non-empty payload
   */
  isPresent(): boolean {
    return this.id < 0;
  }

  uploadToBackend(): Promise<void> {
    return api.post(`/image/${this.id}/update`, instanceToPlain(this));
  }

  getMetadataAsBadges(): Array<Badge> {
    const result: Array<Badge> = [];
    if (this.experiment) {
      result.push(createExperimentBadge(this.experiment));
    }
    if (this.sample) {
      result.push(createSampleBadge(this.sample));
    }
    if (this.timePoint) {
      result.push(createTimePointBadge(this.timePoint));
    }
    if (this.pixelSizeMillimeter && this.pixelSizeMillimeter > 0) {
      result.push(createPixelSizeBadge(`${this.pixelSizeMillimeter} mm`));
    }
    if (this.assayType && this.assayType != AssayType.Unknown) {
      result.push(createAssayTypeBadge(this.assayType));
    }
    return result;
  }

  getAnnotationsAsBadges(): Array<Badge> {
    const result: Array<Badge> = [];
    for (const annotation of this.maskImageAnnotations) {
      if (annotation.version > 0) {
        result.push({
          text: annotation.annotationTypeId,
          icon: 'fa-solid fa-tag',
          color: '#164089',
          type: 'mask-annotation/' + annotation.annotationTypeId
        });
      }
    }
    return result;
  }


}

export function incrementImageMaskAnnotationVersion(image: ImagePayload, annotationType: string) {
  let found : MaskImageAnnotationPayload | null = null
  for(const annotation of image.maskImageAnnotations) {
    if(annotation.annotationTypeId == annotationType) {
      found = annotation;
    }
  }
  if(!found) {
    found = new MaskImageAnnotationPayload()
    found.annotationTypeId = annotationType
    found.imageId = image.id
    found.version = 0
    found.projectId = image.projectId;
  }
  found.version++
}

export function imageSupportsMaskAnnotation(img: ImagePayload, name: string): boolean {
  if(img.assayType == AssayType.DDA && name == "zoi-shape") {
    return false;
  }
  return true;
}
