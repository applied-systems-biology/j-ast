<template>
  <q-input
    v-if="parameter.dataType == BackendTaskWorkloadParameterSlotDataType.String"
    type="text"
    v-model="parameter.value"
    filled
    :label="parameter.label"
  >
    <q-tooltip>{{ parameter.description }}</q-tooltip>
  </q-input>
  <q-input
    v-else-if="
      parameter.dataType == BackendTaskWorkloadParameterSlotDataType.Number
    "
    type="number"
    v-model="parameter.value"
    filled
    :label="parameter.label"
  >
    <q-tooltip>{{ parameter.description }}</q-tooltip>
  </q-input>
  <q-checkbox
    v-else-if="
      parameter.dataType == BackendTaskWorkloadParameterSlotDataType.Boolean
    "
    type="number"
    v-model="parameter.value"
    :label="parameter.label"
  >
    <q-tooltip>{{ parameter.description }}</q-tooltip>
  </q-checkbox>
  <div v-else class="text-red">
    <q-icon name="warning" />
    Unable to render parameter {{ parameter.id }} with type
    {{ parameter.dataType }}
  </div>
</template>
<script setup lang="ts">
import {
  BackendTaskParameterPayload,
  BackendTaskWorkloadParameterSlotDataType,
} from 'src/types/backendTasks';
import { defineModel } from 'vue';

const parameter = defineModel<BackendTaskParameterPayload>({ required: true });
</script>
<style scoped lang="scss"></style>
