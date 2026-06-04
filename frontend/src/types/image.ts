import {Expose, instanceToPlain, plainToInstance} from 'class-transformer';
import {AssayType} from 'src/types/assayType';
import {api} from 'boot/axios';
import {
    Badge,
    createAssayTypeBadge,
    createExperimentBadge,
    createPixelSizeBadge,
    createSampleBadge,
    createStripPresetBadge,
    createTimePointBadge
} from 'src/types/badge';
import {StripPresetPayload} from "src/types/presets";

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
    pixelSizeMillimeter: number = -1;

    @Expose()
    metadata: Record<string, any> = {};

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

    getMetadataAsDict(): Record<string, any> {
        return {
            id: this.id,
            fileName: this.fileName,
            experiment: this.experiment,
            sample: this.sample,
            timePoint: this.timePoint,
            assayType: this.assayType,
            mic: this.mic,
            groupRow: this.groupRow,
            groupColumn: this.groupColumn,
            version: this.version,
            pixelSizeMillimeter: this.pixelSizeMillimeter,
            metadata: this.metadata,
        }
    }

    getMetadataAsFlatDict() : Record<string, any> {
        const a = {
            id: this.id,
            fileName: this.fileName,
            experiment: this.experiment,
            sample: this.sample,
            timePoint: this.timePoint,
            assayType: this.assayType,
            mic: this.mic,
            groupRow: this.groupRow,
            groupColumn: this.groupColumn,
            version: this.version,
            pixelSizeMillimeter: this.pixelSizeMillimeter
        }
        return { ...a, ...this.metadata }
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
        if(this.assayType == AssayType.ETest) {
            const stripPreset = this.getStripPreset()
            if(stripPreset) {
                result.push(createStripPresetBadge(stripPreset.name))
            }
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

    getStripPreset() : StripPresetPayload | undefined {
        const v = this.getMetadata("stripPreset")
        if(v) {
            const instance = plainToInstance(StripPresetPayload, v)
            if (instance.isPresent()) {
                return instance
            }
        }
        return undefined
    }

    getMetadata(name: string) {
        return this.getMetadataAsFlatDict()[name]
    }

    hasMetadata(name: string) {
        const value = this.getMetadataAsFlatDict()[name];
        if(value) {
            if(name == "pixelSizeMillimeter") {
                return value > 0
            }
            else if(name == "stripPreset") {
                try {
                    const instance = plainToInstance(StripPresetPayload, value)
                    return instance.isPresent()
                } catch {
                    return false
                }
            }
            else {
                return true
            }
        }
        return false
    }
}

export function setImageMetadata(image: ImagePayload, key: string, value: any) {
    if(key == "experiment") {
        image.experiment = value;
    }
    else if(key == "sample") {
        image.sample = value;
    }
    else if(key == "timePoint") {
        image.timePoint = value;
    }
    else if(key == "assayType") {
        image.assayType = value;
    }
    else if(key == "mic") {
        image.mic = value;
    }
    else if(key == "pixelSizeMillimeter") {
        image.pixelSizeMillimeter = value;
    }
    else if(key == "stripPreset") {
        image.metadata["stripPreset"] = value;
    }
}

export function incrementImageMaskAnnotationVersion(image: ImagePayload, annotationType: string) {
    let found: MaskImageAnnotationPayload | null = null
    for (const annotation of image.maskImageAnnotations) {
        if (annotation.annotationTypeId == annotationType) {
            found = annotation;
        }
    }
    if (!found) {
        found = new MaskImageAnnotationPayload()
        found.annotationTypeId = annotationType
        found.imageId = image.id
        found.version = 0
        found.projectId = image.projectId;
    }
    found.version++
}

export function imageHasMaskAnnotation(image: ImagePayload, annotationType: string) {
    for (const annotation of image.maskImageAnnotations) {
        if(annotation.annotationTypeId == annotationType) {
            return annotation.version > 0
        }
    }
    return false
}

export function imageSupportsMaskAnnotation(img: ImagePayload, name: string): boolean {
    if (img.assayType == AssayType.DDA && name == "zoi-shape") {
        return false;
    }
    return true;
}

export function imageSupportsMetadata(
    img: ImagePayload,
    name: string
): boolean {
    if (img.assayType == AssayType.DDA && name == 'stripPreset') {
        return false;
    }
    return true;
}
