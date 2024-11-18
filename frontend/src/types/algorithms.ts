import { Dialog } from 'quasar';
import ImageAutofillMetadataDialog from 'components/algorithms/ImageAutofillMetadataDialog.vue';
import {ImagePayload} from "src/types/common";

export interface FrontEndImageProcessorResponse {
  images : ImagePayload[];
  needsUpload: boolean;
  needsFullReload: boolean;
}

export interface FrontEndImageProcessor {
  label: string;
  tooltip: string;
  fn: (images : ImagePayload[]) => Promise<FrontEndImageProcessorResponse>;
}

export function doImageAutofillMetadata(images: ImagePayload[]) : Promise<FrontEndImageProcessorResponse> {
  return new Promise<FrontEndImageProcessorResponse>((resolve, reject) => {
    Dialog.create({
      component: ImageAutofillMetadataDialog,
      componentProps: {
        text: 'something',
        persistent: true,
      }
    }).onOk(() => {

      // TODO: Do something
      for(const image of images) {
        image.experiment = "Test 123"
      }

      resolve({ needsUpload: true, needsFullReload: false, images: images });
    }).onCancel(() => {
      reject()
    }).onDismiss(() => {
      reject()
    })
  })
}

export const frontEndProcessors : Array<FrontEndImageProcessor> = [
  { label: "Auto-fill metadata", tooltip: "Auto-fills metadata from the file name", fn: doImageAutofillMetadata }
]
