<template>
  <div class="q-pa-sm">
    <div class="q-mb-lg">
      Please upload the raw image files here:
    </div>
    <q-btn :disable="uploader?.isBusy" class="full-width q-mb-sm" color="green" icon="upload" @click="uploadNow">Upload now</q-btn>
    <div class="uploader-panel" >
      <q-uploader
        ref="uploader"
        :url="`/api/project/${props.projectId}/upload-raw-image`"
        label="Upload raw data"
        field-name="file"
        multiple
        accept=".png, image/*"
        @rejected="onRejected"
        @failed="onError"
        @uploading="onUpload"
        @finish="onFinished"
        hide-upload-btn
        :headers="[ { name: 'Authorization', value: `Bearer ${authStore.accessToken}` } ]"
      />
    </div>
  </div>

</template>
<script setup lang="ts">
import {QUploader, useQuasar} from "quasar";
import {useAuthStore} from "stores/auth-store";
import {ref} from "vue";

type ValidationError = Array<{ failedPropValidation: string, file: File }>
type UploadError ={ files: readonly any[]; xhr: any; }
const $q = useQuasar()
const authStore = useAuthStore();
const props = defineProps<{
  projectId?: string
}>()
const emit = defineEmits<{
  (e: 'finished'): void
}>();
const uploader = ref<QUploader | null>(null)

function onError(info : UploadError) {
  console.log(info)
}

function onUpload() {
  authStore.doRefreshToken()
}

function onRejected(rejectedEntries : ValidationError) {
  $q.notify({
    type: 'negative',
    message: `${rejectedEntries.length} file(s) did not pass validation constraints`
  })
}

function onFinished() {
  emit('finished')
}

function uploadNow() {
  if(uploader.value && uploader.value.files.length > 0){
    authStore.doRefreshToken()
    uploader.value?.upload()
  }
  else {
    $q.notify({
      type: 'negative',
      message: "Nothing to upload"
    })
  }

}
</script>
<style scoped>
.uploader-panel {
  display: flex;
}

.q-uploader {
  display: flex;
  flex-direction: column!important;
  max-height: none;
  max-width: none;
}
</style>
