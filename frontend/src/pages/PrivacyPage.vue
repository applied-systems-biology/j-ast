<template>
  <q-page class="page-container" padding>
    <div v-html="privacyContent">

    </div>
  </q-page>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import {
  loadStringFromApi,
} from 'src/types/common';
const privacyContent = ref<string>('Loading ...');

onMounted(() => {
  loadStringFromApi('/provider/privacy', privacyContent);
});
</script>
<style scoped></style>
