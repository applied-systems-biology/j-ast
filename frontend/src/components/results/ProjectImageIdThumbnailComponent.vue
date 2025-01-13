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
