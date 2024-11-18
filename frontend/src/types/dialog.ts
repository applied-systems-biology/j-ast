import {Dialog} from "quasar";

export function onDialogYes(title: string, message: string) : Promise<void> {
  return new Promise((resolve, reject) => {
    Dialog.create({
      title: title,
      message: message,
      cancel: {
        label: 'No',
        color: "blue-grey"
      },
      ok: {
        label: 'Yes',
        color: 'red',
      },
      persistent: true,
    }).onOk(() => {
      resolve()
    }).onCancel(() => {
      reject()
    }).onDismiss(() => {
      reject()
    })
  })
}
