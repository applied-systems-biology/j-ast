<template>
  <q-card v-if="hasTaskRunning" class="q-mb-lg">
    <q-card-section>
      <q-icon name="lock" size="l"/>
      <span class="text-caption text-bold">Currently being processed</span>
    </q-card-section>
    <q-card-section> You will not be able to make any changes</q-card-section>
  </q-card>
  <q-btn
      align="left"
      class="w-100"
      color="secondary"
      icon="fa-solid fa-arrow-up-right-from-square"
      label="Show image"
      @click="showImage"
  />
  <q-input
      :disable="hasTaskRunning"
      :model-value="model?.fileName"
      class="w-100"
      filled
      label="File name"
      @update:model-value="onUpdateFileName"
  />
  <q-input
      :disable="hasTaskRunning"
      :model-value="model?.experiment"
      class="w-100"
      filled
      label="Experiment"
      @update:model-value="onUpdateExperiment"
  />
  <q-input
      :disable="hasTaskRunning"
      :model-value="model?.sample"
      class="w-100"
      filled
      label="Sample"
      @update:model-value="onUpdateSample"
  />
  <q-input
      :disable="hasTaskRunning"
      :model-value="model?.timePoint"
      class="w-100"
      filled
      label="Time point"
      @update:model-value="onUpdateTimePoint"
  />
  <q-select
      :disable="hasTaskRunning"
      :model-value="model?.assayType"
      :options="['DDA', 'ETest', 'Unknown']"
      class="w-100"
      filled
      label="Assay type"
      @update:model-value="onUpdateAssayType"
  />
  <q-input
      :disable="hasTaskRunning"
      :model-value="model?.pixelSizeMillimeter"
      class="w-100"
      filled
      label="Pixel size (mm)"
      type="number"
      @update:model-value="onUpdatePixelSize"
  />
  <q-input
      v-if="model?.assayType == 'ETest'"
      :disable="hasTaskRunning"
      :model-value="model?.mic"
      class="w-100"
      filled
      label="MIC"
      type="number"
      @update:model-value="onUpdateMIC"
  />
  <MaskImageAnnotationButton
      v-model="model"
      :disable="hasTaskRunning"
      annotation-type-id="plate"
      class="w-100"
  />
  <MaskImageAnnotationButton
      v-if="model?.assayType != 'Unknown'"
      v-model="model"
      :disable="hasTaskRunning"
      annotation-type-id="strip-disk"
      class="w-100"
  />
  <MaskImageAnnotationButton
      v-if="model && imageSupportsMaskAnnotation(model, 'zoi-shape')"
      v-model="model"
      :disable="hasTaskRunning"
      annotation-type-id="zoi-shape"
      class="w-100"
  />
  <StripPresetAnnotationButton
      v-if="model && imageSupportsMetadata(model, 'stripPreset')"
      v-model="model"
      :disable="hasTaskRunning"
      class="w-100"
  />
</template>
<script lang="ts" setup>
import {debounce} from 'quasar';
import {plainToInstance} from 'class-transformer';
import MaskImageAnnotationButton from 'components/drawers/MaskImageAnnotationButton.vue';
import {sendFailureNotification} from 'src/types/notification';
import {AssayType} from 'src/types/assayType';
import {ImagePayload, imageSupportsMaskAnnotation, imageSupportsMetadata} from 'src/types/image';
import {BackendTaskPayload} from 'src/types/backendTasks';
import {computed} from 'vue';
import {ResultItemPayload, ResultItemType, showResultItem,} from 'src/types/results';
import StripPresetAnnotationButton from "components/drawers/StripPresetAnnotationButton.vue";

type SelectValue = string | number | null;
const model = defineModel<ImagePayload>();
const projectBackendTasks = defineModel<BackendTaskPayload[]>(
    'projectBackendTasks'
);
const hasTaskRunning = computed(() => {
  if (projectBackendTasks.value) {
    for (const task of projectBackendTasks.value) {
      if (task.isRunning()) {
        return true
      }
    }
    return false
  }
  return true; // Waiting still for info
});
const uploadToBackend = debounce(uploadToBackend_, 300);

function showImage() {
  const item = new ResultItemPayload();
  item.type = ResultItemType.Image;
  item.visualizationType = ResultItemType.Null;
  item.overrideUrl = `/image/${model.value?.id}/raw`;
  item.name = model.value?.fileName || 'Unnamed';
  showResultItem(item);
}

function uploadToBackend_() {
  if (model.value) {
    const payload = plainToInstance(ImagePayload, model.value);
    payload.uploadToBackend().catch(() => {
      sendFailureNotification('Error while updating');
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
  if (model.value) {
    model.value.pixelSizeMillimeter = Number(newValue);
    uploadToBackend();
  }
}

function onUpdateMIC(newValue: SelectValue) {
  if (model.value) {
    model.value.mic = Number(newValue);
    uploadToBackend();
  }
}
</script>
<style scoped></style>
