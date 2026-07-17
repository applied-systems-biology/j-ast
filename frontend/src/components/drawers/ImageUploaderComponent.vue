<!--
  - Copyright (c) 2026.
  -
  - Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
  - https://www.leibniz-hki.de/en/applied-systems-biology.html
  - HKI-Center for Systems Biology of Infection
  - Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
  - Adolf-Reichwein-Straße 23, 07745 Jena, Germany
  -
  - The project code is licensed under MIT.
  - See the LICENSE file provided with the code for the full license.
  -->

<template>
  <div class="q-pa-sm">
    <q-banner
        v-if="authStore.isGuest"
        class="text-white bg-red q-mb-md"
        inline-actions
        rounded
    >
      <q-icon name="info"/>
      Please note that you can only have up to {{ authStore.limits.guestMaxImages }} images per project due to the guest
      account restrictions.
    </q-banner>
    <q-btn :disable="uploader?.isBusy" class="full-width q-mb-sm" color="green" icon="upload" label="Upload now" no-caps
           no-wrap @click="uploadNow"/>
    <div class="uploader-panel">
      <q-uploader
          ref="uploader"
          :headers="[ { name: 'Authorization', value: `Bearer ${authStore.accessToken}` } ]"
          :url="`${apiBase}/project/${props.projectId}/upload-raw-image`"
          accept=".png, image/*"
          field-name="file"
          hide-upload-btn
          label="Upload raw data"
          multiple
          no-thumbnails
          @failed="onError"
          @finish="onFinished"
          @rejected="onRejected"
          @uploading="onUpload"
      />
    </div>
  </div>

</template>
<script lang="ts" setup>
import {QUploader} from "quasar";
import {useAuthStore} from "stores/auth-store";
import {useTemplateRef} from "vue";
import {sendFailureNotification} from "src/types/notification";
import {apiBase} from 'src/types/api';

type ValidationError = Array<{ failedPropValidation: string, file: File }>
type UploadError = { files: readonly any[]; xhr: any; }
const authStore = useAuthStore();
const props = defineProps<{
  projectId?: string
}>()
const emit = defineEmits<{
  (e: 'finished'): void
}>();
const uploader = useTemplateRef<QUploader>("uploader")

function onError(info: UploadError) {
  console.log(info)
}

function onUpload() {
  authStore.doRefreshToken()
}

function onRejected(rejectedEntries: ValidationError) {
  sendFailureNotification(`${rejectedEntries.length} file(s) did not pass validation constraints`)
}

function onFinished() {
  emit('finished')
}

function uploadNow() {
  if (uploader.value && uploader.value.files.length > 0) {
    authStore.doRefreshToken()
    uploader.value?.upload()
  } else {
    sendFailureNotification("Nothing to upload")
  }

}
</script>
<style scoped>
.uploader-panel {
  display: flex;
}

.q-uploader {
  display: flex;
  flex-direction: column !important;
  max-height: none;
  max-width: none;
}
</style>
