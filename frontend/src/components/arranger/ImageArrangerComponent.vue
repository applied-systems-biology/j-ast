<template>
  <div class="grid-container" @click="selectedImageIds = []">
    <q-card class="full-width bg-indigo-1" v-if="showUnsorted" flat>
      <q-card-section class="flex row q-gutter-sm">
        <div class="text-bold flex flex-center">Unsorted images</div>
      </q-card-section>

      <!-- Unsorted -->
      <q-card-section>
        <q-scroll-area class="unsorted-scroll-area" visible>
          <div class="grid-row grid-row-unsorted bg-indigo-1">
            <div
              v-for="(k, columnIndex) in (projectImages?.unsortedRow.images
                .length || 0) + 1"
              :key="`col-${columnIndex}`"
              class="grid-slot"
              @dragover.prevent="onDragOver(new SlotIndex(-1, columnIndex))"
              @dragenter.prevent="onDragEnter(new SlotIndex(-1, columnIndex))"
              @dragleave.prevent="onDragLeave(new SlotIndex(-1, columnIndex))"
              @drop="onDrop(new SlotIndex(-1, columnIndex))"
            >
              <ProjectImageButton
                v-if="getUnsortedImage(columnIndex)"
                :has-running-task="imagesWithRunningTasks.has(getUnsortedImage(columnIndex)!.id)"
                :current-image="getUnsortedImage(columnIndex)!"
                :selected-image-ids="selectedImageIds"
                class="draggable-item"
                draggable="true"
                @dragstart="onDragStart(new SlotIndex(-1, columnIndex))"
                @dragend="onDragEnd"
                @image-selected="onImageSelected"
              />
            </div>
          </div>
        </q-scroll-area>
      </q-card-section>

      <q-separator />

      <q-card-actions>
        <q-btn flat @click.stop="selectAllUnsorted">Select all unsorted</q-btn>
      </q-card-actions>

      <q-separator />
    </q-card>

    <q-infinite-scroll class="table-scroll-area q-mt-md" @load="loadNextRows">
      <!-- Sorted -->
      <div class="grid-column-header">
        <div class="grid-row-label"></div>
        <div
          v-for="(k, columnIndex) in numCols"
          :key="`column-label-${columnIndex}`"
          class="grid-column-label bg-indigo-1"
        >
          <q-scroll-area class="column-label-scroll-area">
            <q-badge
              v-for="badge in getColumnBadges(columnIndex)"
              :key="`${badge.type}-${badge.text}`"
              :style="{ backgroundColor: badge.color }"
            >
              <q-icon :name="badge.icon" />
              <span class="q-ml-sm">{{ badge.text }}</span>
            </q-badge>
          </q-scroll-area>
          <q-btn
            flat
            size="xs"
            icon="fa-solid fa-chevron-down"
            @click.stop="selectColumn(columnIndex, $event)"
          >
            <q-tooltip>Select the whole column</q-tooltip>
          </q-btn>
        </div>
      </div>
      <div
        v-for="(i, rowIndex) in numRenderedRows + 1"
        :key="`row-${rowIndex}`"
        class="grid-row bg-indigo-1"
      >
        <div class="grid-row-label">
          <q-scroll-area class="row-label-scroll-area">
            <q-badge
              v-for="badge in getRowBadges(rowIndex)"
              :key="`${badge.type}-${badge.text}`"
              :style="{ backgroundColor: badge.color }"
            >
              <q-icon :name="badge.icon" />
              <span class="q-ml-sm">{{ badge.text }}</span>
            </q-badge>
          </q-scroll-area>
          <q-btn
            flat
            size="xs"
            icon="fa-solid fa-chevron-right"
            class="select-all-button"
            @click.stop="selectRow(rowIndex, $event)"
          >
            <q-tooltip>Select the whole row</q-tooltip>
          </q-btn>
        </div>
        <div
          v-for="(j, columnIndex) in numCols + 1"
          :key="`col-${columnIndex}`"
          class="grid-slot"
          @dragover.prevent="onDragOver(new SlotIndex(rowIndex, columnIndex))"
          @dragenter.prevent="onDragEnter(new SlotIndex(rowIndex, columnIndex))"
          @dragleave.prevent="onDragLeave(new SlotIndex(rowIndex, columnIndex))"
          @drop="onDrop(new SlotIndex(rowIndex, columnIndex))"
        >
          <ProjectImageButton
            v-if="getImageBySlot(rowIndex, columnIndex)"
            :current-image="getImageBySlot(rowIndex, columnIndex)!"
            :selected-image-ids="selectedImageIds"
            :has-running-task="imagesWithRunningTasks.has(getImageBySlot(rowIndex, columnIndex)!.id)"
            class="draggable-item"
            draggable="true"
            @dragstart="onDragStart(new SlotIndex(rowIndex, columnIndex))"
            @dragend="onDragEnd"
            @image-selected="onImageSelected"
          />
        </div>
      </div>
     <q-btn icon="refresh" flat class="q-ma-sm" @click="maxNumRenderedRows += 5" >
       Load more rows ({{ Math.max(0, numRows - numRenderedRows) }} left)
     </q-btn>
    </q-infinite-scroll>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import ProjectImageButton from 'components/arranger/ProjectImageButton.vue';
import { sendFailureNotification } from 'src/types/notification';
import { Badge } from 'src/types/badge';
import {
  ProjectImagesPayload,
  ProjectImagesPayloadRow,
} from 'src/types/projectImages';
import { BackendTaskPayload } from 'src/types/backendTasks';

defineProps<{
  showUnsorted: boolean;
}>();

class SlotIndex {
  row: number;
  column: number;

  constructor(row: number, column: number) {
    this.row = row;
    this.column = column;
  }

  equals(other: SlotIndex): boolean {
    return this.row === other.row && this.column === other.column;
  }

  toString(): string {
    return `Slot[${this.row}, ${this.column}]`;
  }
}

const selectedImageIds = defineModel<Array<number>>('selectedImageIds', {
  required: true,
});
const projectBackendTasks = defineModel<BackendTaskPayload[]>('backendTasks');

const projectImages = defineModel<ProjectImagesPayload>();
const numCols = computed(() =>
  projectImages.value ? projectImages.value.maxColumn() + 1 : 0
);
const numRows = computed(() =>
  projectImages.value ? projectImages.value.groupRows.length : 0
);
const maxNumRenderedRows = ref(0);
const numRenderedRows = computed(() => {
  return Math.min(numRows.value, maxNumRenderedRows.value);
});
const imagesWithRunningTasks = computed(() => {
  const result = new Set<number>();
  if (projectBackendTasks.value) {
    for (const task of projectBackendTasks.value) {
      if (task.isRunning()) {
        task.imageIds.forEach(result.add, result);
      }
    }
  }
  return result;
});
const lastProjectId = ref(-1);

const isDragging = ref(false);
const dragSlot = ref<SlotIndex | null>(null);
const dragOverSlot = ref<SlotIndex | null>(null);

function getUnsortedImage(index: number) {
  return projectImages.value?.unsortedRow.images[index] || undefined;
}

function getImageBySlot(row: number, column: number) {
  return projectImages.value?.getImageBySlot(row, column) || undefined;
}

function getRowBadges(rowIndex: number): Array<Badge> {
  const row: ProjectImagesPayloadRow | undefined =
    projectImages.value?.groupRows[rowIndex];
  if (row) {
    return row!.getRowMetadataAsBadges();
  } else {
    return [];
  }
}

function getColumnBadges(columnIndex: number): Array<Badge> {
  return projectImages.value?.getColumnMetadataAsBadges(columnIndex) || [];
}

function onDragStart(slot: SlotIndex) {
  dragSlot.value = slot;
  isDragging.value = true;
}

function onDragEnd() {
  dragSlot.value = null;
  dragOverSlot.value = null;
  isDragging.value = false;
}

function onDragOver(slot: SlotIndex) {
  if (dragSlot.value !== slot) {
    dragOverSlot.value = slot;
  }
}

function onDragEnter(slot: SlotIndex) {
  dragOverSlot.value = slot;
}

function loadNextRows(index: number, done: (stop: boolean) => void) {
  // console.log('loadNextRows ' + maxNumRenderedRows.value);
  maxNumRenderedRows.value = maxNumRenderedRows.value + 5;
  // const stop = maxNumRenderedRows.value > numRows.value
  done(false);
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
function onDragLeave(slot: SlotIndex) {
  dragOverSlot.value = null;
}

function onDrop(targetSlot: SlotIndex) {
  if (dragSlot.value === null || dragSlot.value === targetSlot) {
    return;
  }
  const sourceSlot: SlotIndex = dragSlot.value;
  if (sourceSlot.equals(targetSlot)) {
    return;
  }
  if (!projectImages.value) {
    return;
  }

  const payload: ProjectImagesPayload = projectImages.value;
  const success = payload.swapOrMove(sourceSlot, targetSlot);
  onDragEnd();
  if (success) {
    projectImages.value?.uploadToBackend().catch(() => {
      sendFailureNotification('Error while updating');
    });
  }
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

function selectAllUnsorted() {
  if (projectImages.value) {
    selectedImageIds.value = projectImages.value?.unsortedRow.images
      .filter((img) => img.groupRow < 0)
      .map((img) => img.id);
  }
}

function selectColumn(columnIndex: number, event: Event) {
  const mouseEvent = event as MouseEvent;
  if (projectImages.value) {
    const indices = new Set<number>();
    if (mouseEvent.shiftKey) {
      for (const id of selectedImageIds.value) {
        indices.add(id);
      }
    }
    for (const row of projectImages.value.groupRows) {
      for (const image of row.images) {
        if (image.groupColumn == columnIndex) {
          indices.add(image.id);
        }
      }
    }
    selectedImageIds.value = [...indices];
  }
}

function selectRow(rowIndex: number, event: Event) {
  const mouseEvent = event as MouseEvent;
  if (projectImages.value) {
    const indices = new Set<number>();
    if (mouseEvent.shiftKey) {
      for (const id of selectedImageIds.value) {
        indices.add(id);
      }
    }
    for (const row of projectImages.value.groupRows) {
      for (const image of row.images) {
        if (image.groupRow == rowIndex) {
          indices.add(image.id);
        }
      }
    }
    selectedImageIds.value = [...indices];
  }
}

onMounted(() => {
  watch(projectImages, () => {
    console.log("new project images:" + projectImages.value?.projectId)
    if(projectImages.value?.projectId != lastProjectId.value) {
      maxNumRenderedRows.value = 0
    }
  });
});
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

.unsorted-scroll-area {
  //border: 5px purple solid;
  width: 100%;
  height: calc($grid-item-height + 2rem);
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

//.grid-row-unsorted {
//  background: blue;
//}

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
