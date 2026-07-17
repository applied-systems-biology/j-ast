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
