import { Dialog } from 'quasar';
import ImageAutofillMetadataDialog from 'components/algorithms/ImageAutofillMetadataDialog.vue';
import { ImagePayload, removeExtensionIfPresent } from 'src/types/common';
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
      .onOk(() => {
        // TODO: Do something
        for (const image of images) {
          image.experiment = 'Test 123';
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
    label: 'Auto-fill metadata',
    icon: 'fa-solid fa-pen-to-square',
    tooltip: 'Auto-fills metadata from the file name',
    fn: doImageAutofillMetadata,
  },
  {
    label: 'Remove file name extensions',
    icon: 'fa-solid fa-pen-to-square',
    tooltip: 'Removes extensions from the file name metadata',
    fn: doImageRemoveFileNameExtension,
  },
];
