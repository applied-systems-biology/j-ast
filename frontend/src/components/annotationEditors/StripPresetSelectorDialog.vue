<template>
  <q-dialog ref="dialogRef" @hide="onDialogHide">
    <q-card class="q-dialog-plugin">
      <q-card-section>
        <div class="text-h6">Set E-Strip preset</div>
      </q-card-section>
      <q-separator />
      <q-card-section>
        <div class="text-bold">
          <q-icon name="help" />
          Info
        </div>
        <div>
         Select the E-strip preset that should be attached to the selected image(s).
        </div>
        <div>To create custom presets, please click the <strong>Presets</strong> button at the top right of the application.</div>
      </q-card-section>
      <q-separator />
      <q-card-section>
        <q-select v-model="payload" :options="stripPresets" filled option-label="name">
          <template v-slot:prepend>
            <q-icon name="fa-solid fa-ruler-vertical" />
          </template>
        </q-select>
      </q-card-section>
      <q-card-section>
        <q-scroll-area v-if="payload" visible class="w-100" style="height: 64px;">
          <StripPreviewComponent :preset="payload" />
        </q-scroll-area>
      </q-card-section>
      <q-card-actions align="right">
        <q-btn no-caps no-wrap color="blue-grey" label="Cancel" @click="onDialogCancel" />
        <q-btn no-caps no-wrap color="red" label="OK" @click="onOKClick" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>
<script setup lang="ts">
import { useDialogPluginComponent } from 'quasar';
import {onMounted, ref} from 'vue';
import {StripPresetPayload} from "src/types/presets";
import {loadPayloadInstanceFromApi} from "src/types/common";
import StripPreviewComponent from "components/utils/StripPreviewComponent.vue";

const stripPresets = ref<StripPresetPayload[]>()
const payload = ref<StripPresetPayload>();

// const props = defineProps({
//   // ...your custom props
// })

defineEmits([
  // REQUIRED; need to specify some events that your
  // component will emit through useDialogPluginComponent()
  ...useDialogPluginComponent.emits,
]);

const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } =
  useDialogPluginComponent();
// dialogRef      - Vue ref to be applied to QDialog
// onDialogHide   - Function to be used as handler for @hide on QDialog
// onDialogOK     - Function to call to settle dialog with "ok" outcome
//                    example: onDialogOK() - no payload
//                    example: onDialogOK({ /*...*/ }) - with payload
// onDialogCancel - Function to call to settle dialog with "cancel" outcome

// this is part of our example (so not required)
function onOKClick() {
  // on OK, it is REQUIRED to
  // call onDialogOK (with optional payload)
  onDialogOK(payload.value);
  // or with payload: onDialogOK({ ... })
  // ...and it will also hide the dialog automatically
}

onMounted(() => {
  loadPayloadInstanceFromApi("/get-presets/strip", StripPresetPayload, stripPresets).then(() => {
    payload.value = stripPresets.value![0]
  })
})


</script>
<style scoped lang="scss">
.q-dialog-plugin {
  width: 700px;
  max-width: 80vw;
}
</style>
