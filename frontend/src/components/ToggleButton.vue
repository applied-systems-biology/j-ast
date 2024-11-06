<template>
  <q-btn :icon="model ? selectedIcon : notSelectedIcon" @click="model = !model">
    <slot/>
  </q-btn>
</template>

<script lang="ts" setup>
import { defineModel } from 'vue';

defineProps<{
  selectedIcon: string;
  notSelectedIcon: string;
}>();

const model = defineModel<boolean>();
</script>


<style scoped>

</style>
