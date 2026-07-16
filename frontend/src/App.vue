<template>
  <router-view />
</template>

<script setup lang="ts">
// Shim required by class-transformer
import 'reflect-metadata';
import { onMounted } from 'vue';

defineOptions({
  name: 'App'
});

onMounted(() => {
  const splash = document.getElementById('app-splash');
  if (splash) {
    splash.remove();
  }
});
</script>
