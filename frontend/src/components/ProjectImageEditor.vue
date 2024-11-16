<template>
  <q-input
    :model-value="model?.fileName"
    filled
    label="File name"
    @update:model-value="onUpdateFileName"
  />
  <q-input
    :model-value="model?.experiment"
    filled
    label="Experiment"
    @update:model-value="onUpdateExperiment"
  />
  <q-input
    :model-value="model?.sample"
    filled
    label="Sample"
    @update:model-value="onUpdateSample"
  />
  <q-input
    :model-value="model?.timePoint"
    filled
    label="Time point"
    @update:model-value="onUpdateTimePoint"
  />
  <q-select
    :model-value="model?.assayType"
    filled
    label="Assay type"
    :options="['DDA', 'ETest', 'Unknown']"
    @update:model-value="onUpdateAssayType"
  />
  <ProjectImageAnnotationButton v-model="model" annotation-type-id="plate" annotation-type-name="Plate"/>
  <ProjectImageAnnotationButton v-if="model?.assayType == 'DDA'" v-model="model" annotation-type-id="strip-disk" annotation-type-name="DDA disk"/>
  <ProjectImageAnnotationButton v-if="model?.assayType == 'ETest'" v-model="model" annotation-type-id="strip-disk" annotation-type-name="ETest strip"/>
  <ProjectImageAnnotationButton v-if="model?.assayType == 'ETest'" v-model="model" annotation-type-id="zoi-shape" annotation-type-name="ETest ZOI shape"/>
</template>
<script setup lang="ts">
import { AssayType, ImagePayload } from 'src/types/common';
import { debounce, useQuasar } from 'quasar';
import { plainToInstance } from 'class-transformer';
import ProjectImageAnnotationButton from 'components/ProjectImageAnnotationButton.vue';

const $q = useQuasar()
type SelectValue = string | number | null;
const model = defineModel<ImagePayload>();
const uploadToBackend = debounce(uploadToBackend_, 300);

function uploadToBackend_() {
  if(model.value) {
    const payload = plainToInstance(ImagePayload, model.value);
    payload.uploadToBackend().catch(() => {
      $q.notify({
        type: 'error',
        message: 'Error while updating',
      });
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
</script>
<style scoped></style>
