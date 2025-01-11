<template>
  <q-skeleton v-if="!props.resultItem" type="rect" class="thumbnail" />
  <q-img
    v-else-if="thumbnail"
    class="thumbnail"
    :src="thumbnail"
    fit="contain"
  />
  <q-icon v-else class="thumbnail" color="grey" name="fa-solid fa-file" size="xl"/>
</template>
<script setup lang="ts">
import {ResultItemPayload, ResultItemType} from "src/types/results";
import {useResultItemThumbnailStore} from "stores/result-item-thumbnail-store";
import {onMounted, ref, watch} from "vue";

const props = defineProps<{
  resultItem: ResultItemPayload
}>()

const thumbnailStore = useResultItemThumbnailStore()
const thumbnail = ref<string>('');

function queryBackend() {
  if (props.resultItem && props.resultItem.type == ResultItemType.Image) {
    thumbnailStore.fetchImage(props.resultItem.id).then(data => {
      thumbnail.value = data || ""
    })
  }
}

onMounted(() => {
  queryBackend()
});
watch(
  () => [props.resultItem],
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
