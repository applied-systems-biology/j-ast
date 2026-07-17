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
  <q-dialog ref="dialogRef" @hide="onDialogHide">
    <q-card class="q-dialog-plugin">
      <q-card-section>
        <div class="text-h6">{{ props.preset ? 'Edit ' : ' Create ' }} E-Strip preset</div>
      </q-card-section>
      <q-separator/>
      <q-card-section class="q-gutter-sm">
        <q-input v-model="payload.name" filled label="Name"/>
        <div><q-icon name="info"/> All tick labels visible on the strip, in descending order. You can separate the ticks via comma/semicolon/newline/tab:</div>
        <q-input v-model="ticksString" class="text-monospace" filled input-class="font-mono" label="Ticks"
                 :rules="[ val => !!parseTicksString(val) || 'Invalid format' ]"
                 type="textarea"/>
      </q-card-section>
      <q-card-section v-if="currentTicks">
        <q-scroll-area v-if="payload" class="w-100" style="height: 64px;" visible>
          <StripPreviewComponent :ticks="currentTicks"/>
        </q-scroll-area>
      </q-card-section>
      <q-card-actions align="right">
        <q-btn color="blue-grey" label="Cancel" no-caps no-wrap @click="onDialogCancel"/>
        <q-btn color="red" label="OK" :disable="!isValid" no-caps no-wrap @click="onOKClick"/>
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>
<script lang="ts" setup>
import {useDialogPluginComponent} from 'quasar';
import {computed, onMounted, ref} from 'vue';
import {StripPresetPayload} from "src/types/presets";
import StripPreviewComponent from "components/utils/StripPreviewComponent.vue";

const payload = ref<StripPresetPayload>(new StripPresetPayload());
const ticksString = ref<string>("")

const props = defineProps<{
  preset?: StripPresetPayload;
}>();
const currentTicks = computed(() => parseTicksString(ticksString.value));

const isValid = computed(() => {
  return currentTicks.value && payload.value.name
})

defineEmits([
  // REQUIRED; need to specify some events that your
  // component will emit through useDialogPluginComponent()
  ...useDialogPluginComponent.emits,
]);

const {dialogRef, onDialogHide, onDialogOK, onDialogCancel} =
    useDialogPluginComponent();
// dialogRef      - Vue ref to be applied to QDialog
// onDialogHide   - Function to be used as handler for @hide on QDialog
// onDialogOK     - Function to call to settle dialog with "ok" outcome
//                    example: onDialogOK() - no payload
//                    example: onDialogOK({ /*...*/ }) - with payload
// onDialogCancel - Function to call to settle dialog with "cancel" outcome


function parseTicksString(str: string): Array<number> | undefined {
  const tokens = str.split(/[\n,; \t]/).map(token => token.trim()).filter(token => token !== '');
  const numbers: number[] = [];

  for (const token of tokens) {
    const num = parseFloat(token);
    if (isNaN(num)) {
      return undefined;
    }
    numbers.push(num);
  }

  if (numbers.length === 0) {
    return undefined;
  }

  for (let i = 1; i < numbers.length; i++) {
    if (numbers[i] > numbers[i - 1]) {
      return undefined;
    }
  }

  return numbers;
}

// this is part of our example (so not required)
function onOKClick() {
  // on OK, it is REQUIRED to
  // call onDialogOK (with optional payload)
  payload.value.ticks = parseTicksString(ticksString.value)!
  onDialogOK(payload.value);
  // or with payload: onDialogOK({ ... })
  // ...and it will also hide the dialog automatically
}

function toTickString(ticks : Array<number>) {
  // Convert the ticks into its string representation
  let str = ""
  for (const tick of ticks) {
    str += tick + "\n"
  }
  return str
}


onMounted(() => {
  if (props.preset) {
    payload.value = props.preset!;
    ticksString.value = toTickString(payload.value.ticks)
  }
})


</script>
<style lang="scss" scoped>
.q-dialog-plugin {
  width: 700px;
  max-width: 80vw;
}
</style>
