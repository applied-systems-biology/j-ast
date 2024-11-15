<template>
  <q-btn
    no-caps
    class="shadow-3 item text-black"
    :color="selectionColor"
    @click="clicked($event)"
  >
    <div class="content q-gutter-sm">
      <q-img
        v-if="thumbnail"
        class="thumbnail"
        :src="thumbnail"
        fit="contain"
      />
      <q-skeleton v-else type="rect" class="thumbnail" />
      <div class="label text-left">
        <div class="filename text-caption ellipsis">
          {{ currentImage.fileName }}
        </div>
        <div class="badges">
          <q-badge v-for="badge in metadataAsBadges" :key="badge.type" :style="{ backgroundColor: badge.color }">
            <q-icon :name="badge.icon" />
            <span class="q-ml-sm">{{ badge.text}}</span>
          </q-badge>
        </div>
        <!-- TODO: will be needed for later -->
        <div class="progress text-indigo" v-if="false">
          <q-spinner-hourglass size="xs"/>
          <span class="text-caption">Working ...</span>
        </div>
      </div>
    </div>
  </q-btn>
</template>
<script setup lang="ts">
import { ImagePayload } from 'src/types/common';
import { computed, onMounted, ref } from 'vue';
import { useProjectImageThumbnailStore } from 'stores/project-image-thumbnail-store';
import { plainToInstance } from 'class-transformer';

const thumbnail = ref<string>('');
const thumbnailStore = useProjectImageThumbnailStore();

const props = defineProps<{
  currentImage: ImagePayload;
  selectedImageIds: Array<number>;
}>();
const emit = defineEmits<{
  (e: 'imageSelected', imageId: number, exclusive: boolean): void;
}>();

const metadataAsBadges = computed(() => plainToInstance(ImagePayload, props.currentImage).getMetadataAsBadges())
const selectionColor = computed(() => {
  const index = props.selectedImageIds.indexOf(props.currentImage.id)
  if(index >= 0) {
    if(index == props.selectedImageIds.length - 1) {
      return "green-3"
    }
    else {
      return "blue-3"
    }
  }
  else {
    return"blue-grey-2";
  }
})

function clicked(event : Event) {
  const mouseEvent = event as MouseEvent;
  if(mouseEvent.shiftKey) {
    emit("imageSelected", props.currentImage.id, false)
  }
  else {
    emit("imageSelected", props.currentImage.id, true)
  }
}

onMounted(() => {
  thumbnailStore.fetchImage(props.currentImage.id).then(data => {
    thumbnail.value = data || ""
  })
});
</script>
<style scoped lang="scss">
$thumbnail-size: 6rem;

.thumbnail {
  width: $thumbnail-size;
  height: $thumbnail-size;
}

.label {
  width: 10rem;
  height: $thumbnail-size;
  display: flex;
  flex-direction: column;
}

.badges {
  flex-grow: 1;
  overflow: hidden;

  .q-badge {
    margin: 2px;
    font-size: 0.6rem;
  }
}

.content {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.text-caption {
  font-size: 0.6rem;
}

.q-icon {
  font-size: 0.6rem;
}
</style>
