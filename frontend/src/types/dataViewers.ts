import {Dialog} from "quasar";
import ImageViewerDialog from "components/dataViewers/DataViewerDialog.vue";

export function showImageViewer(imageBackendUrl: string) {
  Dialog.create({
    component: ImageViewerDialog,
    componentProps: {
      imageBackendUrl: imageBackendUrl,
      persistent: true,
    },
  })
    .onOk(() => {
    })
    .onCancel(() => {
    })
    .onDismiss(() => {
    });
}
