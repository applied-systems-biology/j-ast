import { Dialog } from 'quasar';
import ImageAutofillMetadataDialog, {
  ImageAutofillMetadataDialogPayload
} from 'components/algorithms/ImageAutofillMetadataDialog.vue';
import { ImagePayload, parseAssayType, removeExtensionIfPresent } from 'src/types/common';
import { onDialogYes } from 'src/types/dialog';

export interface FrontEndImageProcessorResponse {
  images: ImagePayload[];
  needsUpload: boolean;
  needsFullReload: boolean;
}

export interface FrontEndImageProcessor {
  label: string;
  icon: string;
  tooltip: string;
  fn: (images: ImagePayload[]) => Promise<FrontEndImageProcessorResponse>;
}

export function doImageAutofillMetadata(
  images: ImagePayload[]
): Promise<FrontEndImageProcessorResponse> {
  return new Promise<FrontEndImageProcessorResponse>((resolve, reject) => {
    Dialog.create({
      component: ImageAutofillMetadataDialog,
      componentProps: {
        text: 'something',
        persistent: true,
      },
    })
      .onOk((payload : ImageAutofillMetadataDialogPayload) => {
        if(!payload.delimiter) {
          return reject(new Error('No delimiter was provided'));
        }
        for (const image of images) {
          if(image.fileName) {
            let fileName = image.fileName;
            if(payload.removeFileExtension) {
              fileName = removeExtensionIfPresent(fileName);
            }
            if(fileName) {
              const elements = fileName.split(payload.delimiter);
              for(const fieldPayload of payload.fields) {
                if(fieldPayload.enabled && fieldPayload.index >= 0 && fieldPayload.index < elements.length) {
                  const currentValue = (image as any)[fieldPayload.fieldName];
                  if(payload.overrideExisting || !currentValue) {
                    // Read out the current value
                    let newValue = elements[fieldPayload.index]

                    // Special case for assay Type
                    if(fieldPayload.fieldName == "assayType") {
                      newValue = parseAssayType(newValue)
                    }

                    (image as any)[fieldPayload.fieldName] = newValue;
                  }
                }
              }
            }
          }
        }
        resolve({ needsUpload: true, needsFullReload: false, images: images });
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
        for(const image of images) {
          image.fileName = removeExtensionIfPresent(image.fileName)
        }
        resolve({ needsUpload: true, needsFullReload: false, images: images });
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
];
