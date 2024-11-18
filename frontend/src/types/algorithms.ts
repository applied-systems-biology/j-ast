import { Dialog } from 'quasar';
import ImageAutofillMetadataDialog from 'components/algorithms/ImageAutofillMetadataDialog.vue';

export interface FrontEndImageProcessor {
  label: string;
  tooltip: string;
  fn: (imageIds : number[]) => Promise<void>;
}

export function doImageAutofillMetadata() : Promise<void> {
  return new Promise<void>((resolve, reject) => {
    Dialog.create({
      component: ImageAutofillMetadataDialog,

      // props forwarded to your custom component
      componentProps: {
        text: 'something',
        persistent: true,
        // ...more..props...
      }
    }).onOk(() => {

      // TODO: Do something
      resolve()
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
