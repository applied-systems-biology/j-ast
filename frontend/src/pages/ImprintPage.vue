<template>
  <q-page class="page-container" padding>
    <div v-html="imprintContent">

    </div>
  </q-page>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import {
  loadStringFromApi,
} from 'src/types/common';
const imprintContent = ref<string>('Loading ...');

onMounted(() => {
  loadStringFromApi('/provider/imprint', imprintContent);
});
</script>
<style scoped></style>
