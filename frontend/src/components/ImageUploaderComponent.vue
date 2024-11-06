<script setup lang="ts">
import {useQuasar} from "quasar";

const $q = useQuasar()
function onRejected(rejectedEntries : Array<{ failedPropValidation: string, file: File }>) {
  $q.notify({
    type: 'negative',
    message: `${rejectedEntries.length} file(s) did not pass validation constraints`
  })
}
</script>

<template>
  <div class="q-pa-sm">
    <div>
      Please upload the raw image files here:
    </div>
    <div class="uploader-panel">
      <q-uploader
        url="http://localhost:4444/upload"
        label="Upload raw data"
        multiple
        accept=".png, image/*"
        @rejected="onRejected"
      />
    </div>
  </div>

</template>

<style scoped>
.uploader-panel {
  display: flex;
}

.q-uploader {
  display: flex;
  max-height: none;
  max-width: none;
}
</style>
