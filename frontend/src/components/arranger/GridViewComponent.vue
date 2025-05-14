<template>
  <div class="grid-container" @click="selectedImageIds = []">
    <q-infinite-scroll class="table-scroll-area q-mt-md q-ml-md" @load="loadNextImages">
      <div style="max-width: calc(100vw - 350px);" class="q-gutter-md">
        <template
          v-for="(i, imageIndex) in numRenderedImages"
          :key="`image-id-${imageIndex}`"
        >
          <ProjectImageButton
            v-if="filterAppliesToImageIndex(imageIndex)"
            :current-image="getImageByIndex(imageIndex)!"
            :selected-image-ids="selectedImageIds"
            class="draggable-item"
            @image-selected="onImageSelected"
            :disabled="hasTaskRunning"
          />
        </template>
        <q-btn
          icon="refresh"
          flat
          class="q-ma-sm"
          @click="maxNumRenderedImages += 30"
        >
          Load more images ({{ Math.max(0, numImages - numRenderedImages) }} left)
        </q-btn>
      </div>
    </q-infinite-scroll>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import ProjectImageButton from 'components/arranger/ProjectImageButton.vue';
import {
  ProjectImagesPayload,
} from 'src/types/projectImages';
import {
  BackendTaskPayload,
} from 'src/types/backendTasks';
import { filterAppliesToImage } from 'src/types/filters';

const selectedImageIds = defineModel<Array<number>>('selectedImageIds', {
  required: true,
});
const projectBackendTasks = defineModel<BackendTaskPayload[]>('backendTasks');
const projectImages = defineModel<ProjectImagesPayload>();
const filterText = defineModel<string>('filterText');

const numImages = computed(() =>
  projectImages.value ? projectImages.value.imageIds.length : 0
);
const maxNumRenderedImages = ref(10);
const numRenderedImages = computed(() => {
  return Math.min(numImages.value, maxNumRenderedImages.value);
});

const hasTaskRunning = computed(() => {
  if (projectBackendTasks.value) {
    for (const task of projectBackendTasks.value) {
      if (task.isRunning()) {
        return true;
      }
    }
    return false;
  }
  return true; // Waiting still for info
});
const lastProjectId = ref(-1);

function filterAppliesToImageIndex(imageIndex: number) {
  if (filterText.value && filterText.value.length > 0 && projectImages.value) {
    const imageId = projectImages.value.imageIds[imageIndex]
    if(!imageId) {
      return true;
    }
    const image = projectImages.value.imagesById[imageId];
    if (!image) {
      return true;
    }
    return filterAppliesToImage(filterText.value, image)
  } else {
    return true;
  }
}

function getImageByIndex(imageIndex: number) {
  if(!projectImages.value) {
    return undefined;
  }
  const imageId = projectImages.value.imageIds[imageIndex]
  return projectImages.value.imagesById[imageId];
}

function loadNextImages(index: number, done: (stop: boolean) => void) {
  maxNumRenderedImages.value = maxNumRenderedImages.value + 5;
  done(false);
}

function onImageSelected(imageId: number, exclusive: boolean) {
  const isMulti = selectedImageIds.value.length > 1;
  const alreadySelected = selectedImageIds.value.includes(imageId);
  if (alreadySelected) {
    if (exclusive && isMulti) {
      // Select only that image
      selectedImageIds.value = [imageId];
    } else {
      // Remove from selection
      selectedImageIds.value.splice(selectedImageIds.value.indexOf(imageId), 1);
    }
  } else {
    if (exclusive) {
      // Select only that image
      selectedImageIds.value = [imageId];
    } else {
      selectedImageIds.value.push(imageId);
    }
  }
}

function selectAll() {
  const newSelected : Array<number> = []
  if(projectImages.value) {
    for(const imageId of projectImages.value.imageIds) {
      if(filterAppliesToImageIndex(imageId)) {
        newSelected.push(imageId);
      }
    }
  }
  selectedImageIds.value = newSelected
}

onMounted(() => {
  watch(projectImages, () => {
    if (projectImages.value?.projectId != lastProjectId.value) {
      maxNumRenderedImages.value = 10;
    }
  });
});

defineExpose({ selectAll })

</script>
<style scoped lang="scss">
$grid-item-width: 18rem;
$grid-item-height: 8rem;
$grid-row-label-width: 10rem;
$grid-column-label-height: 5rem;

.select-all-button {
  padding: 4px;
}

.grid-row-label {
  width: $grid-row-label-width;
  border-right: 1px solid #ccc;
  height: 100%;
  padding: 4px;
  display: flex;
  flex-direction: row;

  .q-badge {
    margin: 2px;
  }
}

.row-label-scroll-area {
  width: 100%;
  height: 100%;
  flex-grow: 1;
}

.grid-column-label {
  width: $grid-item-width;
  height: $grid-column-label-height;
  display: flex;
  flex-direction: column;
  margin-left: 10px;
  border-top-left-radius: 3px;
  border-top-right-radius: 3px;
  padding: 4px;

  .q-badge {
    margin: 2px;
  }
}

.column-label-scroll-area {
  width: 100%;
  height: 100%;
  flex-grow: 1;
}

.grid-column-header {
  display: flex;
  flex-direction: row;
  margin-left: 10px;
  padding-left: 10px;
  margin-bottom: 10px;

  .grid-row-label {
    border: none;
  }
}

.grid-container {
  //border: 1px red solid;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow: visible;
}

.table-scroll-area {
  //border: 5px purple solid;
  width: 100%;
  height: 0;
  flex-grow: 1;
}

.grid-row {
  //background: green;
  height: calc($grid-item-height + 1rem);
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  margin-left: 10px;
  padding-left: 10px;
  padding-right: 10px;
  margin-right: 10px;
  overflow: visible;
  border-radius: 3px;
}

.grid-slot {
  width: $grid-item-width;
  height: $grid-item-height;
  border: 2px dashed #ccc;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: visible;
}

.grid-slot.dragging-over {
  border-color: $purple;
}

.draggable-item {
  width: $grid-item-width;
  height: $grid-item-height;
  cursor: grab;
}
</style>
