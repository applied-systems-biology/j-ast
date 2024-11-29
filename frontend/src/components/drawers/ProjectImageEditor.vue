<template>
  <q-card class="q-mb-lg" v-if="hasTaskRunning">
    <q-card-section>
      <q-spinner-hourglass size="md"/>
      <span class="text-caption text-bold">Currently being processed</span>
    </q-card-section>
    <q-card-section>
      You will not be able to make any changes
    </q-card-section>
  </q-card>
  <q-btn icon="fa-solid fa-arrow-up-right-from-square" align="left" color="secondary" label="Show image" class="w-100" @click="showImage"/>
  <q-input
    :model-value="model?.fileName"
    :disable="hasTaskRunning"
    filled
    label="File name"
    @update:model-value="onUpdateFileName"
  />
  <q-input
    :model-value="model?.experiment"
    :disable="hasTaskRunning"
    filled
    label="Experiment"
    @update:model-value="onUpdateExperiment"
  />
  <q-input
    :model-value="model?.sample"
    :disable="hasTaskRunning"
    filled
    label="Sample"
    @update:model-value="onUpdateSample"
  />
  <q-input
    :model-value="model?.timePoint"
    :disable="hasTaskRunning"
    filled
    label="Time point"
    @update:model-value="onUpdateTimePoint"
  />
  <q-select
    :model-value="model?.assayType"
    :disable="hasTaskRunning"
    filled
    label="Assay type"
    :options="['DDA', 'ETest', 'Unknown']"
    @update:model-value="onUpdateAssayType"
  />
  <q-input
    :model-value="model?.pixelSizeMillimeter"
    :disable="hasTaskRunning"
    filled
    label="Pixel size (mm)"
    type="number"
    @update:model-value="onUpdatePixelSize"
  />
  <MaskImageAnnotationButton :disable="hasTaskRunning" v-model="model" annotation-type-id="plate"/>
  <MaskImageAnnotationButton :disable="hasTaskRunning" v-if="model?.assayType != 'Unknown'" v-model="model" annotation-type-id="strip-disk"/>
  <MaskImageAnnotationButton :disable="hasTaskRunning" v-if="model?.assayType == 'ETest'" v-model="model" annotation-type-id="zoi-shape"/>
</template>
<script setup lang="ts">
import { debounce } from 'quasar';
import { plainToInstance } from 'class-transformer';
import MaskImageAnnotationButton from 'components/drawers/MaskImageAnnotationButton.vue';
import {sendFailureNotification} from "src/types/notification";
import { AssayType } from 'src/types/assayType';
import { ImagePayload } from 'src/types/image';
import {BackendTaskPayload, imageHasRunningTask} from "src/types/backendTasks";
import {computed} from "vue";
import {showImageViewer} from "src/types/dataViewers";

type SelectValue = string | number | null;
const model = defineModel<ImagePayload>();
const projectBackendTasks = defineModel<BackendTaskPayload[]>("projectBackendTasks");
const hasTaskRunning = computed(() => {
  return imageHasRunningTask(model.value?.id, projectBackendTasks.value);
})
const uploadToBackend = debounce(uploadToBackend_, 300);

function showImage() {
  showImageViewer(`/image/${model.value?.id}/raw`)
}

function uploadToBackend_() {
  if(model.value) {
    const payload = plainToInstance(ImagePayload, model.value);
    payload.uploadToBackend().catch(() => {
      sendFailureNotification('Error while updating')
    });
  }
}

function onUpdateFileName(newValue: SelectValue) {
  if (model.value) {
    model.value.fileName = '' + newValue;
    uploadToBackend();
  }
}

function onUpdateExperiment(newValue: SelectValue) {
  if (model.value) {
    model.value.experiment = '' + newValue;
    uploadToBackend();
  }
}

function onUpdateSample(newValue: SelectValue) {
  if (model.value) {
    model.value.sample = '' + newValue;
    uploadToBackend();
  }
}

function onUpdateTimePoint(newValue: SelectValue) {
  if (model.value) {
    model.value.timePoint = '' + newValue;
    uploadToBackend();
  }
}

function onUpdateAssayType(newValue: SelectValue) {
  if (model.value) {
    model.value.assayType = ('' + newValue) as AssayType;
    uploadToBackend();
  }
}

function onUpdatePixelSize(newValue: SelectValue) {
  if(model.value) {
    model.value.pixelSizeMillimeter = Number(newValue);
    uploadToBackend();
  }
}
</script>
<style scoped></style>
