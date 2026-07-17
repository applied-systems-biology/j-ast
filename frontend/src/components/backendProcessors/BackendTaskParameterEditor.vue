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
