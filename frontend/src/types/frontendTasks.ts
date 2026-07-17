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

import { Dialog } from 'quasar';
import ImageAutofillMetadataDialog, {
  ImageAutofillMetadataDialogFieldPayloadMode,
  ImageAutofillMetadataDialogPayload,
} from 'components/frontendProcessors/ImageAutofillMetadataDialog.vue';
import { removeExtensionIfPresent } from 'src/types/common';
import { onDialogYes } from 'src/types/dialog';
import ImageAutoSortByMetadataDialog, {
  ImageAutoSortByMetadataDialogPayload,
} from 'components/frontendProcessors/ImageAutoSortByMetadataDialog.vue';
import {
  sendFailureNotification,
  sendSuccessNotification,
} from 'src/types/notification';
import { AssayType, parseAssayType } from 'src/types/assayType';
import { ImagePayload, setImageMetadata, isReservedMetadataKey, getCustomMetadataValue, setCustomMetadata } from 'src/types/image';
import { ProjectImagesPayload } from 'src/types/projectImages';
import { splitByDelimiters } from 'src/types/utils';
import { ViewMode } from 'src/types/view';
import { StripPresetPayload } from 'src/types/presets';
import { api } from 'boot/axios';
import { plainToInstance } from 'class-transformer';

const NAMED_FIELDS = ['assayType', 'experiment', 'sample', 'timePoint'];

export interface FrontEndImageProcessorResponse {
  images: ImagePayload[];
  needsUpload: boolean;
  needsFullReload: boolean;
}

export interface FrontEndImageProcessor {
  label: string;
  icon: string;
  tooltip: string;
  viewMode: ViewMode | undefined;
  fn: (images: ImagePayload[], state: ProjectImagesPayload) => Promise<FrontEndImageProcessorResponse>;
}

export function doImageAutofillMetadata(
  images: ImagePayload[]
): Promise<FrontEndImageProcessorResponse> {
  return new Promise<FrontEndImageProcessorResponse>((resolve, reject) => {
    Dialog.create({
      component: ImageAutofillMetadataDialog,
      componentProps: {
        persistent: true,
      },
    })
      .onOk((payload: ImageAutofillMetadataDialogPayload) => {
        if (!payload.delimiter) {
          return reject(new Error('No delimiter was provided'));
        }
        for (const image of images) {
          if (image.fileName) {
            let fileName = image.fileName;
            if (payload.removeFileExtension) {
              fileName = removeExtensionIfPresent(fileName);
            }
            if (fileName) {
              const elements = splitByDelimiters(fileName, payload.delimiter);
              for (const fieldPayload of payload.fields) {

                if(fieldPayload.mode == ImageAutofillMetadataDialogFieldPayloadMode.Dynamic) {
                  const zindex = fieldPayload.index - 1
                  if (zindex >= 0 && zindex < elements.length) {
                    const isNamed = NAMED_FIELDS.includes(fieldPayload.fieldName);
                    if (isNamed) {
                      const currentValue = (image as any)[fieldPayload.fieldName];
                      if (payload.overrideExisting || !currentValue) {
                        // Read out the current value
                        let newValue = elements[zindex]

                        // Special case for assay Type
                        if (fieldPayload.fieldName == "assayType") {
                          newValue = parseAssayType(newValue)
                        }

                        (image as any)[fieldPayload.fieldName] = newValue;
                      }
                    } else if (!isReservedMetadataKey(fieldPayload.fieldName)) {
                      // Custom metadata
                      const currentValue = getCustomMetadataValue(image, fieldPayload.fieldName);
                      if (payload.overrideExisting || !currentValue) {
                        setCustomMetadata(image, fieldPayload.fieldName, elements[zindex]);
                      }
                    }
                  }
                }
                else if(fieldPayload.mode == ImageAutofillMetadataDialogFieldPayloadMode.Static) {
                  const isNamed = NAMED_FIELDS.includes(fieldPayload.fieldName);
                  if (isNamed) {
                    const currentValue = (image as any)[fieldPayload.fieldName];
                    if (payload.overrideExisting || !currentValue) {
                      let newValue = fieldPayload.staticValue

                      // Special case for assay Type
                      if (fieldPayload.fieldName == "assayType") {
                        newValue = parseAssayType(newValue)
                      }

                      (image as any)[fieldPayload.fieldName] = newValue;
                    }
                  } else if (!isReservedMetadataKey(fieldPayload.fieldName)) {
                    // Custom metadata
                    const currentValue = getCustomMetadataValue(image, fieldPayload.fieldName);
                    if (payload.overrideExisting || !currentValue) {
                      setCustomMetadata(image, fieldPayload.fieldName, fieldPayload.staticValue);
                    }
                  }
                }

              }
            }
          }
        }
        resolve({needsUpload: true, needsFullReload: false, images: images});
      })
      .onCancel(() => {
        reject();
      })
      .onDismiss(() => {
        reject();
      });
  });
}

export function doImageAutoSortByMetadata(
  images: ImagePayload[],
  projectImages: ProjectImagesPayload,
): Promise<FrontEndImageProcessorResponse> {
  return new Promise<FrontEndImageProcessorResponse>((resolve, reject) => {
    Dialog.create({
      component: ImageAutoSortByMetadataDialog,
      componentProps: {
        images: images,
        projectImages: projectImages,
        persistent: true,
      },
    })
      .onOk((payload: ImageAutoSortByMetadataDialogPayload) => {
        if (payload.timePointOrder.length > 0) {
          // Find the column indices (assign timePoint to an index)
          const columnIndicesPlus1 : Record<string, number> = {}
          let maxColumn = projectImages.maxColumn()
          let minSearchColumnIndex = 0;
          for(const requestedTimePoint of payload.timePointOrder) {

            let newColumnIndex = maxColumn + 1

            // Starting from the minimum search column we look for a column that only contains the requested time point
            for (let columnIndex = minSearchColumnIndex; columnIndex <= maxColumn; columnIndex++) {
              const uniqueMetadata = projectImages.getUniqueColumnMetadata(columnIndex)
              if(uniqueMetadata["timePoint"]) {
                const columnTimePoint = uniqueMetadata["timePoint"];
                if(columnTimePoint == requestedTimePoint) {
                  newColumnIndex = columnIndex
                  break
                }
              }
            }

            // We have a new column index -> assign in map + block (min search)
            columnIndicesPlus1[requestedTimePoint] = newColumnIndex + 1;
            minSearchColumnIndex = newColumnIndex + 1
            maxColumn = Math.max(maxColumn, newColumnIndex)
          }

          if(Object.keys(columnIndicesPlus1).length == 0) {
            sendFailureNotification("Unable to find time point columns!")
            reject()
            return
          }

          let numSuccess = 0
          let numFailed = 0

          // Assign rows
          for(const image of images) {
            if(image.groupRow < 0) {
              // We find a row where everything fits
              let newRow = projectImages.groupRows.length // Default to next row
              const columnIndex = (columnIndicesPlus1[image.timePoint || ""] || 0) - 1 // We correct for the column index


              if(columnIndex < 0) {
                numFailed++
                continue
              }

              for (let rowIndex = 0; rowIndex < projectImages.groupRows.length; rowIndex++) {
                const rowPayload = projectImages.groupRows[rowIndex]
                const uniqueMetadata = rowPayload.getUniqueRowMetadata()

                // Check if the row metadata matches
                if((image.experiment || "") == (uniqueMetadata["experiment"] || "") &&
                  (image.sample || "") == (uniqueMetadata["sample"] || "") &&
                  (image.assayType || "") == (uniqueMetadata["assayType"] || "")) {

                  // Check if the column is empty -> if not, we refuse to sort

                  if(columnIndex >= 0) {
                    const existingImage = rowPayload.getImageByColumn(columnIndex)
                    if(!existingImage) {
                      // Success!
                      newRow = rowIndex
                      break
                    }
                  }
                }
              }

              const indexInUnsorted = projectImages.unsortedRow.images.indexOf(image)
              if(indexInUnsorted >= 0) {
                // Assign the row
                if(projectImages.swapOrMove({ row: image.groupRow, column: indexInUnsorted }, { row: newRow, column: columnIndex})) {
                  numSuccess++
                }
                else {
                  // console.log("r:", image.fileName, " -> ", { row: image.groupRow, indexInUnsorted }, " -> ", { row: newRow, column: columnIndex})
                  numFailed++
                }
              }
              else {
                numFailed++
              }
            }
          }

          sendSuccessNotification(`Successfully sorted ${numSuccess} images (${numFailed} rejected)`)
          resolve({needsUpload: true, needsFullReload: false, images: images});
        } else {
          sendFailureNotification("No time point order provided. Unable to sort!")
          reject()
        }

      })
      .onCancel(() => {
        reject();
      })
      .onDismiss(() => {
        reject();
      });
  });
}

export function doImageRemoveFileNameExtension(
  images: ImagePayload[]
): Promise<FrontEndImageProcessorResponse> {
  return new Promise<FrontEndImageProcessorResponse>((resolve, reject) => {
    onDialogYes(
      'Remove file name extensions',
      "This will remove known image extensions (png/bmp/jpg/jpeg/tif/tiff) from the 'File name' metadata."
    )
      .then(() => {
        for (const image of images) {
          image.fileName = removeExtensionIfPresent(image.fileName)
        }
        resolve({needsUpload: true, needsFullReload: false, images: images});
      })
      .catch(reject);
  });
}

export function doImageEraseMetadata(images: ImagePayload[]): Promise<FrontEndImageProcessorResponse> {
  return new Promise<FrontEndImageProcessorResponse>((resolve, reject) => {
    onDialogYes(
      'Remove all metadata',
      "The following metadata values will be removed: assay type, experiment, sample, time point."
    )
      .then(() => {
        for (const image of images) {
          image.assayType = AssayType.Unknown
          image.experiment = ""
          image.sample = ""
          image.timePoint = ""
        }
        resolve({needsUpload: true, needsFullReload: false, images: images});
      })
      .catch(reject);
  })
}

export function doImageUnsort(images: ImagePayload[]): Promise<FrontEndImageProcessorResponse> {
  return new Promise<FrontEndImageProcessorResponse>((resolve, reject) => {
    onDialogYes(
      'Unsort selected images',
      "The selected images will be moved back into the 'Unsorted' drawer."
    )
      .then(() => {
        for (const image of images) {
          image.groupColumn = -1
          image.groupRow = -1
        }
        resolve({needsUpload: true, needsFullReload: true, images: images});
      })
      .catch(reject);
  })
}

export function doImageMatchStripPresets(
  images: ImagePayload[],
  projectImages: ProjectImagesPayload
): Promise<FrontEndImageProcessorResponse> {
  return new Promise<FrontEndImageProcessorResponse>((resolve, reject) => {
    const eTestImages = projectImages.getAllImages().filter(
      (img) => img.assayType === AssayType.ETest
    );
    if (eTestImages.length === 0) {
      sendFailureNotification('No E-Test images found in project');
      reject();
      return;
    }

    api.get("/get-presets/strip").then((response) => {
      const stripPresets = plainToInstance(StripPresetPayload, response.data as StripPresetPayload[]);
      if (stripPresets.length === 0) {
        sendFailureNotification('No strip presets found in the preset library');
        reject();
        return;
      }

      let matched = 0;
      let alreadyMatched = 0;

      for (const image of eTestImages) {
        const raw = image.metadata?.stripPreset;
        if (!raw) continue;
        const imported = plainToInstance(StripPresetPayload, raw);
        if (!imported.isPresent()) continue;

        if (imported.name && imported.id > 0) {
          alreadyMatched++;
          continue;
        }

        for (const preset of stripPresets) {
          if (preset.ticksMatch(imported)) {
            setImageMetadata(image, "stripPreset", preset);
            image.version += 1;
            matched++;
            break;
          }
        }
      }

      if (matched === 0) {
        sendFailureNotification(
          `No unmatched strip presets could be matched to the library. (${alreadyMatched} already matched, ${eTestImages.length - alreadyMatched - matched} no match found)`
        );
        reject();
        return;
      }

      sendSuccessNotification(
        `Matched ${matched} strip preset(s) to the library. (${alreadyMatched} already matched, ${eTestImages.length - alreadyMatched - matched} no match found)`
      );
      resolve({needsUpload: true, needsFullReload: true, images: images});
    }).catch(() => {
      sendFailureNotification('Failed to load strip presets from server');
      reject();
    });
  });
}

export const frontEndImageProcessors: Array<FrontEndImageProcessor> = [
  {
    label: 'Remove file name extensions',
    icon: 'fa-solid fa-gear',
    tooltip: 'Removes extensions from the file name metadata',
    viewMode: undefined,
    fn: doImageRemoveFileNameExtension,
  },
  {
    label: 'Auto-fill metadata',
    icon: 'fa-solid fa-pen-to-square',
    tooltip: 'Auto-fills metadata from the file name',
    viewMode: undefined,
    fn: doImageAutofillMetadata,
  },
  {
    label: 'Auto-sort by metadata',
    icon: 'fa-solid fa-shuffle',
    tooltip: 'Moves unsorted images into a slot that fits best',
    viewMode: ViewMode.Timeline,
    fn: doImageAutoSortByMetadata,
  },
  {
    label: 'Clear text metadata',
    icon: 'fa-solid fa-eraser',
    tooltip: 'Clears all text metadata except the file name. Does not affect mask/annotation metadata!',
    viewMode: undefined,
    fn: doImageEraseMetadata,
  },
  {
    label: 'Move to unsorted',
    icon: 'fa-solid fa-eraser',
    tooltip: 'Moves the selected images back into the "Unsorted" drawer',
    viewMode: ViewMode.Timeline,
    fn: doImageUnsort,
  },
  {
    label: 'Match strip presets to library',
    icon: 'fa-solid fa-ruler-vertical',
    tooltip: 'Matches E-Test strip presets with unknown names to the preset library by comparing tick sequences',
    viewMode: undefined,
    fn: doImageMatchStripPresets,
  },
];
