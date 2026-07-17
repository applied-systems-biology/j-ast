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

import {Expose, instanceToPlain, plainToInstance} from 'class-transformer';
import {AssayType} from 'src/types/assayType';
import {api} from 'boot/axios';
import {
    Badge,
    createAssayTypeBadge,
    createCustomMetadataBadge,
    createExperimentBadge,
    createPixelSizeBadge,
    createSampleBadge,
    createStripPresetBadge,
    createTimePointBadge
} from 'src/types/badge';
import {StripPresetPayload} from "src/types/presets";

export const RESERVED_METADATA_KEYS: Set<string> = new Set([
  'experiment', 'sample', 'timePoint', 'assayType', 'mic',
  'pixelSizeMillimeter', 'stripPreset', 'groupRow', 'groupColumn',
  'version', 'fileName', 'id', 'owner',
]);

export function isReservedMetadataKey(key: string): boolean {
  return RESERVED_METADATA_KEYS.has(key);
}

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
        const flatMetadata: Record<string, any> = {};
        for (const [key, val] of Object.entries(this.metadata || {})) {
            if (key === 'stripPreset') {
                flatMetadata[key] = val;  // keep full object
            } else if (val && typeof val === 'object' && 'value' in val) {
                flatMetadata[key] = val.value;  // unwrap custom metadata
            } else {
                flatMetadata[key] = val;  // legacy scalar
            }
        }
        return { ...a, ...flatMetadata };
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
                result.push(createStripPresetBadge(stripPreset.getName()))
            }
        }
        if (this.assayType && this.assayType != AssayType.Unknown) {
            result.push(createAssayTypeBadge(this.assayType));
        }
        // Custom metadata badges
        for (const key of getCustomMetadataKeys(this)) {
            const entry = this.metadata[key];
            if (entry && typeof entry === 'object' && entry.showBadge === true) {
                result.push(createCustomMetadataBadge(key, String(entry.value)));
            }
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
    else {
        // Custom metadata: write as object-based value
        image.metadata[key] = { ...(image.metadata[key] || {}), value };
    }
}

export function getCustomMetadataKeys(image: ImagePayload): string[] {
    return Object.keys(image.metadata || {}).filter(
        key => key !== 'stripPreset' && !isReservedMetadataKey(key)
    );
}

export function getCustomMetadataValue(image: ImagePayload, key: string): any {
    const entry = image.metadata?.[key];
    if (entry && typeof entry === 'object' && 'value' in entry) {
        return entry.value;
    }
    // Legacy scalar fallback
    return entry;
}

export function setCustomMetadata(image: ImagePayload, key: string, value: any, showBadge?: boolean) {
    if (isReservedMetadataKey(key)) {
        throw new Error(`'${key}' is a reserved metadata key`);
    }
    const existing = image.metadata[key];
    const isObject = existing && typeof existing === 'object' && !Array.isArray(existing);
    image.metadata[key] = {
        ...(isObject ? existing : {}),
        value,
        ...(showBadge !== undefined ? { showBadge } : {}),
    };
}

export function setCustomMetadataBadgeVisibility(image: ImagePayload, key: string, showBadge: boolean) {
    if (isReservedMetadataKey(key)) return;
    const existing = image.metadata[key];
    if (existing && typeof existing === 'object') {
        existing.showBadge = showBadge;
    } else if (existing !== undefined) {
        image.metadata[key] = { value: existing, showBadge };
    }
}

export function deleteCustomMetadata(image: ImagePayload, key: string) {
    if (isReservedMetadataKey(key)) return;
    delete image.metadata[key];
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
