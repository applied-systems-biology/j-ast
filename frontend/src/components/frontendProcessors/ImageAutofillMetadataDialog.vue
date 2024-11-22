<template>
  <q-dialog ref="dialogRef" @hide="onDialogHide">
    <q-card class="q-dialog-plugin">
      <q-card-section>
        <div class="text-h6">Auto-fill metadata</div>
      </q-card-section>
      <q-separator />
      <q-card-section>
        <div class="text-bold">
          <q-icon name="help" />
          Info
        </div>
        <div>
          The file name will be split by the delimiter and the metadata values
          will be extracted as the n-th element (starting with 0) from the
          resulting list.
        </div>
        <div>
          For example the file name <code>DDA_e1_d13_48hr.png</code> will be
          split into <code>DDA</code>, <code>e1</code>, <code>d13</code>, and
          <code>48hr.png</code>.
        </div>
        <div>
          You can also remove the file extension if you just want
          <code>48hr</code>.
        </div>
      </q-card-section>
      <q-card-section>
        <q-toggle
          v-model="payload.removeFileExtension"
          label="Remove file extension"
        />
        <q-input
          v-model="payload.delimiter"
          filled
          label="Delimiter"
          :rules="[(val) => !!val || 'Cannot be empty']"
        />
      </q-card-section>
      <q-separator />
      <q-card-section class="row q-gutter-sm">
        <q-card class="col-5" v-for="value in payload.fields" :key="value.fieldName">
          <q-card-section class="row">
            <div class="col text-bold">{{ value.label }}</div>
            <q-toggle v-model="value.enabled" />
          </q-card-section>
          <q-separator />
          <q-card-section>
            <q-input
              :disable="!value.enabled"
              type="number"
              label="Element index"
              v-model="value.index"
              filled
              :rules="[(val) => val >= 0 || 'Most be at least zero']"
            />
          </q-card-section>
        </q-card>
      </q-card-section>
      <q-card-section>
        <q-toggle
          v-model="payload.overrideExisting"
          label="Overwrite existing values"
        />
      </q-card-section>
      <q-separator />
      <q-card-actions align="right">
        <q-btn color="blue-grey" label="Cancel" @click="onDialogCancel" />
        <q-btn color="red" label="OK" @click="onOKClick" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>
<script lang="ts">
export interface ImageAutofillMetadataDialogFieldPayload {
  fieldName: string;
  label: string;
  enabled: boolean;
  index: number;
}

export interface ImageAutofillMetadataDialogPayload {
  delimiter: string;
  overrideExisting: boolean;
  removeFileExtension: boolean;
  fields: ImageAutofillMetadataDialogFieldPayload[];
}
</script>
<script setup lang="ts">
import { useDialogPluginComponent } from 'quasar';
import { ref } from 'vue';

const payload = ref<ImageAutofillMetadataDialogPayload>({
  delimiter: '_',
  overrideExisting: true,
  removeFileExtension: true,
  fields: [
    {
      fieldName: 'assayType',
      label: 'Assay type',
      enabled: true,
      index: 0,
    },
    {
      fieldName: 'experiment',
      label: 'Experiment',
      enabled: true,
      index: 1,
    },
    {
      fieldName: 'sample',
      label: 'Sample',
      enabled: true,
      index: 2,
    },
    {
      fieldName: 'timePoint',
      label: 'Time point',
      enabled: true,
      index: 3,
    },
  ],
});

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
</script>
<style scoped lang="scss">
.q-dialog-plugin {
  width: 700px;
  max-width: 80vw;
}
</style>
