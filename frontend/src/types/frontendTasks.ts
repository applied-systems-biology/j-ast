import { Dialog } from 'quasar';
import ImageAutofillMetadataDialog, { ImageAutofillMetadataDialogFieldPayloadMode, ImageAutofillMetadataDialogPayload } from 'components/frontendProcessors/ImageAutofillMetadataDialog.vue';
import { removeExtensionIfPresent } from 'src/types/common';
import { onDialogYes } from 'src/types/dialog';
import ImageAutoSortByMetadataDialog, { ImageAutoSortByMetadataDialogPayload } from 'components/frontendProcessors/ImageAutoSortByMetadataDialog.vue';
import { sendFailureNotification, sendSuccessNotification } from 'src/types/notification';
import { parseAssayType } from 'src/types/assayType';
import { ImagePayload } from 'src/types/image';
import { ProjectImagesPayload } from 'src/types/projectImages';
import { splitByDelimiters } from 'src/types/utils';

export interface FrontEndImageProcessorResponse {
  images: ImagePayload[];
  needsUpload: boolean;
  needsFullReload: boolean;
}

export interface FrontEndImageProcessor {
  label: string;
  icon: string;
  tooltip: string;
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
                  if (fieldPayload.index >= 0 && fieldPayload.index < elements.length) {
                    const currentValue = (image as any)[fieldPayload.fieldName];
                    if (payload.overrideExisting || !currentValue) {
                      // Read out the current value
                      let newValue = elements[fieldPayload.index]

                      // Special case for assay Type
                      if (fieldPayload.fieldName == "assayType") {
                        newValue = parseAssayType(newValue)
                      }

                      (image as any)[fieldPayload.fieldName] = newValue;
                    }
                  }
                }
                else if(fieldPayload.mode == ImageAutofillMetadataDialogFieldPayloadMode.Static) {
                  const currentValue = (image as any)[fieldPayload.fieldName];
                  if (payload.overrideExisting || !currentValue) {
                    let newValue = fieldPayload.staticValue

                    // Special case for assay Type
                    if (fieldPayload.fieldName == "assayType") {
                      newValue = parseAssayType(newValue)
                    }

                    (image as any)[fieldPayload.fieldName] = newValue;
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

export const frontEndImageProcessors: Array<FrontEndImageProcessor> = [
  {
    label: 'Remove file name extensions',
    icon: 'fa-solid fa-gear',
    tooltip: 'Removes extensions from the file name metadata',
    fn: doImageRemoveFileNameExtension,
  },
  {
    label: 'Auto-fill metadata',
    icon: 'fa-solid fa-pen-to-square',
    tooltip: 'Auto-fills metadata from the file name',
    fn: doImageAutofillMetadata,
  },
  {
    label: 'Auto-sort by metadata',
    icon: 'fa-solid fa-shuffle',
    tooltip: 'Moves unsorted images into a slot that fits best',
    fn: doImageAutoSortByMetadata,
  },
];
