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
  <q-skeleton v-if="!thumbnail" type="rect" class="thumbnail" />
  <q-img
    v-else-if="thumbnail"
    class="thumbnail"
    :src="thumbnail"
    fit="contain"
  />
</template>
<script setup lang="ts">
import {onMounted, ref, watch} from "vue";
import { useProjectImageThumbnailStore } from "stores/project-image-thumbnail-store";

const props = defineProps<{
  imageId: number
}>()

const thumbnailStore = useProjectImageThumbnailStore()
const thumbnail = ref<string>('');

function queryBackend() {
  if (props.imageId) {
    thumbnailStore.fetchImage(props.imageId, -1).then(data => {
      thumbnail.value = data || ""
    })
  }
}

onMounted(() => {
  queryBackend()
});
watch(
  () => [props.imageId],
  () => {
    queryBackend();
  }
);
</script>
<style scoped lang="scss">
.thumbnail {
  width: 100px;
  height: 100px;
}
</style>
