<template>
  <router-view />
</template>

<script setup lang="ts">
// Shim required by class-transformer
import 'reflect-metadata';

defineOptions({
  name: 'App'
});
</script>
