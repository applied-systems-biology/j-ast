<template>
  <div class="grid-container">
    <div class="grid-row grid-row-unsorted">
      <div
        v-for="(k, columnIndex) in ((projectImages?.unsortedRow.images.length || 0) + 1)"
        :key="`col-${columnIndex}`"
        class="grid-slot"
        :class="{ 'dragging-over': isDragging && dragOverSlot?.equals(new SlotIndex(-1, columnIndex)) }"
        @dragover.prevent="onDragOver(new SlotIndex(-1, columnIndex))"
        @dragenter.prevent="onDragEnter(new SlotIndex(-1, columnIndex))"
        @dragleave.prevent="onDragLeave(new SlotIndex(-1, columnIndex))"
        @drop="onDrop(new SlotIndex(-1, columnIndex))">
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
    <div v-for="(i, rowIndex) in (numRows + 1)" :key="`row-${rowIndex}`" class="grid-row">
      <div
        v-for="(j, columnIndex) in (numCols + 1)"
        :key="`col-${columnIndex}`"
        class="grid-slot"
        :class="{ 'dragging-over': isDragging && dragOverSlot?.equals(new SlotIndex(rowIndex, columnIndex)) }"
        @dragover.prevent="onDragOver(new SlotIndex(rowIndex, columnIndex))"
        @dragenter.prevent="onDragEnter(new SlotIndex(rowIndex, columnIndex))"
        @dragleave.prevent="onDragLeave(new SlotIndex(rowIndex, columnIndex))"
        @drop="onDrop(new SlotIndex(rowIndex, columnIndex))"
        @clicked="onImageClicked"
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
  </div>
  <pre>
    {{ JSON.stringify(projectImages, null, 2) }}
  </pre>

</template>
<script setup lang="ts">
import { computed, ref } from 'vue';
import { ProjectImagesPayload } from 'src/types/common';
import ProjectImageButton from 'components/ProjectImageButton.vue';
import { useQuasar } from 'quasar';

const emit = defineEmits<{
  (e: "selectedImageChanged", selectedImageId: number) : void
}>()

class SlotIndex {
  row: number;
  column : number;

  constructor(row : number, column : number) {
    this.row = row;
    this.column  = column;
  }

  equals(other: SlotIndex): boolean {
    return this.row === other.row && this.column === other.column;
  }

  toString() : string {
    return `Slot[${this.row}, ${this.column}]`
  }
}

const $q = useQuasar()
const projectImages = defineModel<ProjectImagesPayload>()
const selectedImageId = ref<number>(-1)
const numCols = computed(() => projectImages.value ? projectImages.value.maxColumn() + 1 : 0)
const numRows = computed(() => projectImages.value ? projectImages.value.groupRows.length : 0)

const isDragging = ref(false);
const dragSlot = ref<SlotIndex | null>(null);
const dragOverSlot = ref<SlotIndex | null>(null);

function getUnsortedImage(index : number) {
  return projectImages.value?.unsortedRow.images[index] || undefined;
}

function getImageBySlot(row : number, column : number) {
  return projectImages.value?.getImageBySlot(row, column) || undefined
}

function onDragStart(slot: SlotIndex){
  dragSlot.value = slot;
  isDragging.value = true;
}

function onDragEnd() {
  dragSlot.value = null;
  dragOverSlot.value = null;
  isDragging.value = false;
}

function onDragOver (slot: SlotIndex) {
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
  const sourceSlot : SlotIndex = dragSlot.value;
  if(sourceSlot.equals(targetSlot)) {
    return
  }
  if(!projectImages.value) {
    return;
  }

  const payload : ProjectImagesPayload = projectImages.value
  const success = payload.swapOrMove(sourceSlot, targetSlot)
  onDragEnd();
  if(success) {
    projectImages.value?.uploadToBackend().catch(() => {
      $q.notify({
        "type": "error",
        "message": "Error while updating",
      })
    })
  }
}

function onImageClicked(imageId : number) {
    if(selectedImageId.value == imageId) {
      selectedImageId.value = -1;
    }
    else {
      selectedImageId.value = imageId;
    }
    emit("selectedImageChanged", selectedImageId.value);
}

</script>
<style scoped lang="scss">

$grid-item-width: 10rem;
$grid-item-height: 16rem;

.grid-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.grid-row {
  background: green;
  height: $grid-item-height;
  display: flex;
  flex-direction: row;
  gap: 10px;
}

.grid-row-unsorted {
  background: blue;
}

.grid-slot {
  width: $grid-item-width;
  height: $grid-item-height;
  border: 2px dashed #ccc;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
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
