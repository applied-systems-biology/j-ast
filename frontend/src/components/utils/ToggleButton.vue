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
  <q-btn no-caps no-wrap :flat="props.flat" :icon="model ? selectedIcon : notSelectedIcon" @click="model = !model" :label="label">
    <slot />
  </q-btn>
</template>

<script lang="ts" setup>
import { defineModel } from 'vue';

const props = defineProps<{
  label?: string;
  selectedIcon: string;
  notSelectedIcon: string;
  flat?: boolean;
}>();

const model = defineModel<boolean>( { required: true });
</script>

<style scoped></style>
