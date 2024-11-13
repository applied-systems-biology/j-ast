<template>
  <q-input :model-value="model?.fileName" filled label="File name" @update:model-value="onUpdateFileName"/>
  <q-input :model-value="model?.experiment" filled label="Experiment" @update:model-value="onUpdateExperiment"/>
  <q-input :model-value="model?.sample" filled label="Sample" @update:model-value="onUpdateSample"/>
  <q-input :model-value="model?.timePoint" filled label="Time point" @update:model-value="onUpdateTimePoint"/>
  <q-select :model-value="model?.assayType" filled label="Assay type" :options="['DDA', 'ETest', 'Unknown']" @update:model-value="onUpdateAssayType"/>
</template>
<script setup lang="ts">
import {AssayType, ImagePayload} from "src/types/common";

type SelectValue = string | number | null
const model = defineModel<ImagePayload>()


function onUpdateFileName(newValue: SelectValue) {
  if(model.value) {
    model.value.fileName = "" + newValue
  }
}

function onUpdateExperiment(newValue: SelectValue) {
  if(model.value) {
    model.value.experiment = "" + newValue
  }
}

function onUpdateSample(newValue: SelectValue) {
  if(model.value) {
    model.value.sample = "" + newValue
  }
}

function onUpdateTimePoint(newValue: SelectValue) {
  if(model.value) {
    model.value.timePoint = "" + newValue
  }
}

function onUpdateAssayType(newValue: SelectValue) {
  if(model.value) {
    model.value.assayType = ("" + newValue) as AssayType
  }
}

</script>
<style scoped>

</style>
