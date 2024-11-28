<template>
  <q-btn :flat="props.flat" :icon="model ? selectedIcon : notSelectedIcon" @click="model = !model">
    <slot />
  </q-btn>
</template>

<script lang="ts" setup>
import { defineModel } from 'vue';

const props = defineProps<{
  selectedIcon: string;
  notSelectedIcon: string;
  flat?: boolean;
}>();

const model = defineModel<boolean>();
</script>

<style scoped></style>
