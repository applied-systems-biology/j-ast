<template>
  <q-btn no-wrap :flat="props.flat" :icon="model ? selectedIcon : notSelectedIcon" @click="model = !model" :label="label">
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
