<template>
  <div class="fullscreen bg-blue text-white text-center q-pa-md flex flex-center">
    <div>
      <div style="font-size: 30vh">
        404
      </div>

      <div class="text-h2" style="opacity:.4">
        Error
      </div>
      <div>
        Current route: {{ $route.fullPath }}
      </div>
      <q-btn
        class="q-mt-xl"
        color="white"
        text-color="blue"
        unelevated
        to="/"
        label="Go Home"
        no-caps
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router';

const $route = useRoute()
defineOptions({
  name: 'ErrorNotFound'
});
</script>
