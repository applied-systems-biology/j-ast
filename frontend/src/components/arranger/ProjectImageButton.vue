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
  <q-btn
      :color="selectionColor" :ripple="false"
      align="left"
      class="shadow-3 item text-black"
      no-caps
      no-wrap
      @click.stop="clicked($event)"
  >
    <div class="content q-gutter-sm">
      <q-img
          v-if="thumbnail"
          :src="thumbnail"
          class="thumbnail"
          fit="contain"
      />
      <q-skeleton v-else class="thumbnail" type="rect"/>
      <div class="label text-left">
        <div class="filename text-caption ellipsis">
          {{ currentImage.fileName }}
        </div>
        <div class="badges">
          <q-badge v-for="badge in metadataAsBadges" :key="`${badge.type}-${badge.text}`"
                   :style="{ backgroundColor: badge.color }">
            <q-icon :name="badge.icon"/>
            <span class="q-ml-sm">{{ badge.text }}</span>
          </q-badge>
          <q-badge v-for="badge in annotationsAsBadges" :key="`${badge.type}-${badge.text}`"
                   :style="{ backgroundColor: badge.color }">
            <q-icon :name="badge.icon"/>
            <span class="q-ml-sm">{{ renderMaskAnnotationId(props.currentImage, badge.text) }}</span>
          </q-badge>
        </div>
      </div>
    </div>
  </q-btn>
</template>
<script lang="ts" setup>
import {renderMaskAnnotationId} from 'src/types/common';
import {computed, onMounted, ref, watch} from 'vue';
import {useProjectImageThumbnailStore} from 'stores/project-image-thumbnail-store';
import {plainToInstance} from 'class-transformer';
import {ImagePayload} from 'src/types/image';

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
const annotationsAsBadges = computed(() => plainToInstance(ImagePayload, props.currentImage).getAnnotationsAsBadges())
const selectionColor = computed(() => {
  const index = props.selectedImageIds.indexOf(props.currentImage.id)
  if (index >= 0) {
    if (index == props.selectedImageIds.length - 1) {
      return "blue-5"
    } else {
      return "blue-3"
    }
  } else {
    return "blue-grey-2";
  }
})

function clicked(event: Event) {
  const mouseEvent = event as MouseEvent;
  if (mouseEvent.shiftKey) {
    emit("imageSelected", props.currentImage.id, false)
  } else {
    emit("imageSelected", props.currentImage.id, true)
  }
}

function queryBackend() {
  // console.log(props.currentImage.fileName, "thumbnail lookup v", props.currentImage.version)
  thumbnailStore.fetchImage(props.currentImage.id, props.currentImage.version).then(data => {
    thumbnail.value = data || ""
  })
}

onMounted(() => {
  queryBackend()
});
watch(
    () => [props.currentImage.id, props.currentImage.version],
    () => {
      queryBackend();
    }
);
</script>
<style lang="scss" scoped>
$thumbnail-size: 6rem;
$label-size: 13rem;

.thumbnail {
  width: $thumbnail-size;
  height: $thumbnail-size;
}

.label {
  width: $label-size;
  height: $thumbnail-size;
  display: flex;
  flex-direction: column;
}

.badges {
  flex-grow: 1;
  overflow: hidden;
  line-height: 1em;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  align-content: flex-start;

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
