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
      <q-card-section v-if="props.existing">
        <div class="text-bold">Current</div>
        <q-scroll-area v-if="props.existing" visible class="w-100" style="height: 64px;">
          <StripPreviewComponent :ticks="props.existing.getEffectiveTicks()" />
        </q-scroll-area>
        <q-separator/>
        <div class="w-100 text-center">
          <q-icon name="fa-solid fa-chevron-down" />
        </div>
        <q-separator/>
      </q-card-section>
      <q-card-section v-if="isExistingNotInLibrary">
        <q-banner class="bg-warning text-black" rounded>
          <template v-slot:avatar>
            <q-icon name="warning" />
          </template>
          This strip preset is not in the preset library.
          <template v-slot:action>
            <q-btn no-caps no-wrap color="primary" label="Create as new preset" @click="createPresetFromExisting" />
          </template>
        </q-banner>
      </q-card-section>
      <q-card-section>
        <q-select v-model="payload" :options="stripPresets" filled option-label="name">
          <template v-slot:prepend>
            <q-icon name="fa-solid fa-ruler-vertical" />
          </template>
        </q-select>
      </q-card-section>
      <q-card-section>
        <div class="text-bold">Preview</div>
        <q-scroll-area v-if="payload" visible class="w-100" style="height: 64px;">
          <StripPreviewComponent :ticks="payload.getEffectiveTicks()" />
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
import {Dialog, useDialogPluginComponent} from 'quasar';
import {computed, onMounted, ref} from 'vue';
import {StripPresetPayload} from "src/types/presets";
import {loadPayloadInstanceFromApi} from "src/types/common";
import StripPreviewComponent from "components/utils/StripPreviewComponent.vue";
import {api} from 'boot/axios';
import {instanceToPlain, plainToInstance} from 'class-transformer';
import {ImagePayload, setImageMetadata} from 'src/types/image';
import {AssayType} from 'src/types/assayType';
import {sendFailureNotification, sendSuccessNotification} from 'src/types/notification';

const stripPresets = ref<StripPresetPayload[]>([])
const payload = ref<StripPresetPayload>();

const props = defineProps<{
  existing?: StripPresetPayload;
  projectImages?: any;
}>();

const isExistingNotInLibrary = computed(() => {
  if (!props.existing || !props.existing.isPresent()) return false;
  if (!stripPresets.value || stripPresets.value.length === 0) return true;
  return !stripPresets.value.some(p => p.ticksMatch(props.existing!));
});

defineEmits([
  ...useDialogPluginComponent.emits,
]);

const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } =
  useDialogPluginComponent();

function onOKClick() {
  onDialogOK(payload.value);
}

function createPresetFromExisting() {
  const newPreset = new StripPresetPayload();
  newPreset.ticks = props.existing!.getEffectiveTicks();
  newPreset.name = props.existing!.name || '';

  Dialog.create({
    title: 'Create new strip preset',
    message: 'Enter a name for the new preset:',
    prompt: {
      model: newPreset.name,
      type: 'text',
    },
    cancel: true,
    persistent: true,
  }).onOk((name: string) => {
    newPreset.name = name;
    api.post("/add-preset/strip", instanceToPlain(newPreset)).then((response) => {
      const created = plainToInstance(StripPresetPayload, response.data);
      sendSuccessNotification(`Created new strip preset "${created.name}"`);

      return loadPayloadInstanceFromApi("/get-presets/strip", StripPresetPayload, stripPresets).then(() => {
        payload.value = stripPresets.value.find(p => p.id === created.id) || stripPresets.value[0];

        if (props.projectImages) {
          updateAllMatchingETests(newPreset, created);
        }
      });
    }).catch(() => {
      sendFailureNotification('Failed to create strip preset');
    });
  });
}

function updateAllMatchingETests(sourcePreset: StripPresetPayload, libraryPreset: StripPresetPayload) {
  const allImages = props.projectImages.getAllImages();
  const matchingImages: ImagePayload[] = [];
  for (const img of allImages) {
    if (img.assayType === AssayType.ETest && img.metadata?.stripPreset) {
      const existing = plainToInstance(StripPresetPayload, img.metadata.stripPreset);
      if (existing.isPresent() && existing.ticksMatch(sourcePreset)) {
        matchingImages.push(img);
      }
    }
  }

  if (matchingImages.length === 0) return;

  Dialog.create({
    title: 'Update matching E-Tests',
    message: `Found ${matchingImages.length} E-Test image(s) with the same tick sequence. Update them all to use "${libraryPreset.name}"?`,
    cancel: true,
    persistent: true,
  }).onOk(() => {
    for (const img of matchingImages) {
      setImageMetadata(img, "stripPreset", libraryPreset);
      img.version += 1;
      plainToInstance(ImagePayload, img).uploadToBackend().catch(() => {});
    }
    sendSuccessNotification(`Updating ${matchingImages.length} image(s) to use "${libraryPreset.name}"`);
  });
}

onMounted(() => {
  loadPayloadInstanceFromApi("/get-presets/strip", StripPresetPayload, stripPresets).then(() => {
    if (props.existing && props.existing.isPresent()) {
      const match = stripPresets.value.find(p => p.ticksMatch(props.existing!));
      payload.value = match || stripPresets.value[0];
    } else {
      payload.value = stripPresets.value[0];
    }
  })
})
</script>
<style scoped lang="scss">
.q-dialog-plugin {
  width: 700px;
  max-width: 80vw;
}
</style>
