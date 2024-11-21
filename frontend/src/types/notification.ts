import {Notify} from "quasar";

export function sendSuccessNotification(message: string) {
  Notify.create({
    type: "positive",
    message: message
  })
}

export function sendInfoNotification(message: string) {
  Notify.create({
    type: "info",
    message: message
  })
}

export function sendFailureNotification(message: string) {
  Notify.create({
    type: "negative",
    message: message
  })
}
