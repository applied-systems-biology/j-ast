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
