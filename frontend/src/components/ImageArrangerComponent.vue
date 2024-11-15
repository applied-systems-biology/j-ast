<template>
  <div class="grid-container">
    <q-card class="full-width bg-indigo-1" v-if="showUnsorted" flat>
      <q-card-section class="flex row q-gutter-sm">
        <div class="text-bold flex flex-center">Unsorted images</div>
      </q-card-section>

      <!-- Unsorted -->
      <q-card-section >
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
                :current-image="getUnsortedImage(columnIndex)!"
                :selected-image-id="selectedImageId"
                class="draggable-item"
                draggable="true"
                @dragstart="onDragStart(new SlotIndex(-1, columnIndex))"
                @dragend="onDragEnd"
                @clicked="onImageClicked"
              />
            </div>
          </div>
        </q-scroll-area>
      </q-card-section>

      <q-separator />

      <q-card-actions>
        <q-btn flat>Action 1</q-btn>
        <q-btn flat>Action 2</q-btn>
      </q-card-actions>

      <q-separator/>
    </q-card>

    <q-scroll-area class="table-scroll-area q-mt-md" visible>
      <!-- Sorted -->
      <div class="grid-column-header">
        <div class="grid-row-label">a</div>
        <div v-for="(k, columnIndex) in numCols" :key="`column-label-${columnIndex}`" class="grid-column-label bg-indigo-1">

        </div>
      </div>
      <div
        v-for="(i, rowIndex) in numRows + 1"
        :key="`row-${rowIndex}`"
        class="grid-row bg-indigo-1"
      >
        <div class="grid-row-label">
          {{ rowIndex }}
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
            :selected-image-id="selectedImageId"
            class="draggable-item"
            draggable="true"
            @dragstart="onDragStart(new SlotIndex(rowIndex, columnIndex))"
            @dragend="onDragEnd"
            @clicked="onImageClicked"
          />
        </div>
      </div>
    </q-scroll-area>
  </div>
</template>
<script setup lang="ts">
import { computed, ref } from 'vue';
import { ProjectImagesPayload } from 'src/types/common';
import ProjectImageButton from 'components/ProjectImageButton.vue';
import { useQuasar } from 'quasar';

const emit = defineEmits<{
  (e: 'selectedImageChanged', selectedImageId: number): void;
}>();
defineProps<{
  showUnsorted: boolean;
}>()

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

const $q = useQuasar();
const projectImages = defineModel<ProjectImagesPayload>();
const selectedImageId = ref<number>(-1);
const numCols = computed(() =>
  projectImages.value ? projectImages.value.maxColumn() + 1 : 0
);
const numRows = computed(() =>
  projectImages.value ? projectImages.value.groupRows.length : 0
);

const isDragging = ref(false);
const dragSlot = ref<SlotIndex | null>(null);
const dragOverSlot = ref<SlotIndex | null>(null);

function getUnsortedImage(index: number) {
  return projectImages.value?.unsortedRow.images[index] || undefined;
}

function getImageBySlot(row: number, column: number) {
  return projectImages.value?.getImageBySlot(row, column) || undefined;
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
      $q.notify({
        type: 'error',
        message: 'Error while updating',
      });
    });
  }
}

function onImageClicked(imageId: number) {
  if (selectedImageId.value == imageId) {
    selectedImageId.value = -1;
  } else {
    selectedImageId.value = imageId;
  }
  emit('selectedImageChanged', selectedImageId.value);
}
</script>
<style scoped lang="scss">
$grid-item-width: 18rem;
$grid-item-height: 8rem;
$grid-row-label-width: 10rem;
$grid-column-label-height: 5rem;

.grid-row-label {
  width: $grid-row-label-width;
  border-right: 1px solid #ccc;
  height: 100%;
}

.grid-column-label {
  width: $grid-item-width;
  height: $grid-column-label-height;
  margin-left: 10px;
  border-top-left-radius: 3px;
  border-top-right-radius: 3px;
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
