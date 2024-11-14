<template>
  <q-btn
    no-caps
    class="shadow-3 item text-black"
    :color="selectedImageId === currentImage.id ? 'green-3' : 'blue-grey-2'"
    @click="clicked"
  >
    <div class="content">
      <q-img
        v-if="thumbnail"
        class="thumbnail"
        :src="thumbnail"
        fit="contain"
      />
      <q-skeleton v-else type="rect" class="thumbnail" />
      <div class="label text-left">
        <div class="text-caption ellipsis">
          {{ currentImage.fileName }}
        </div>
        <div
          v-if="currentImage.experiment"
          class="text-caption ellipsis text-blue-grey"
        >
          {{ currentImage.experiment }}
        </div>
        <div
          v-if="!currentImage.experiment"
          class="text-caption ellipsis text-blue-grey"
        >
          <i>&lt;No experiment&gt;</i>
        </div>
        <div
          v-if="currentImage.sample"
          class="text-caption ellipsis text-blue-grey"
        >
          {{ currentImage.sample }}
        </div>
        <div
          v-if="!currentImage.sample"
          class="text-caption ellipsis text-blue-grey"
        >
          <i>&lt;No sample&gt;</i>
        </div>
        <div
          v-if="currentImage.timePoint"
          class="text-caption ellipsis text-blue-grey"
        >
          {{ currentImage.timePoint }}
        </div>
        <div
          v-if="!currentImage.timePoint"
          class="text-caption ellipsis text-blue-grey"
        >
          <i>&lt;No time point&gt;</i>
        </div>
        <div
          v-if="currentImage.assayType"
          class="text-caption ellipsis text-blue-grey"
        >
          {{ currentImage.assayType }}
        </div>
        <div
          v-if="!currentImage.assayType"
          class="text-caption ellipsis text-blue-grey"
        >
          <i>&lt;No assay type&gt;</i>
        </div>
        {{ currentImage.groupRow }} {{ currentImage.groupColumn }}
      </div>
    </div>
  </q-btn>
</template>
<script setup lang="ts">
import { ImagePayload } from 'src/types/common';
import { onMounted, ref } from 'vue';
import { useProjectImageThumbnailStore } from 'stores/project-image-thumbnail-store';

const thumbnail = ref<string>('');
const thumbnailStore = useProjectImageThumbnailStore();

const props = defineProps<{
  currentImage: ImagePayload;
  selectedImageId: number;
}>();
const emit = defineEmits<{
  (e: 'clicked', imageId: number): void;
}>();

function clicked() {
  emit('clicked', props.currentImage.id);
}

onMounted(() => {
  thumbnailStore.fetchImage(props.currentImage.id).then(data => {
    thumbnail.value = data || ""
  })
});
</script>
<style scoped lang="scss">
$grid-item-size: 9rem;

.thumbnail {
  width: $grid-item-size;
  height: $grid-item-size;
}

.content {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: calc($grid-item-size - 1rem);
}

.text-caption {
  width: calc($grid-item-size - 1rem);
  font-size: 0.7rem;
}
</style>
