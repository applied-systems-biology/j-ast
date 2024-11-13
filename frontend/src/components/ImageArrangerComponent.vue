<template>
  <div class="grid-container">
    <div class="grid-row grid-row-unsorted">

    </div>
    <div v-for="rowIndex in (numRows + 1)" :key="`row-${rowIndex}`" class="grid-row">
      <div
        v-for="columnIndex in (numCols + 1)"
        :key="`col-${columnIndex}`"
        class="grid-slot"
        :class="{ 'dragging-over': isDragging && dragOverSlot === index }"
        @dragover.prevent="onDragOver(index)"
        @dragenter.prevent="onDragEnter(index)"
        @dragleave.prevent="onDragLeave(index)"
        @drop="onDrop(index)"
      >
<!--        <div-->
<!--          v-if="item"-->
<!--          class="draggable-item"-->
<!--          draggable="true"-->
<!--          @dragstart="onDragStart(index)"-->
<!--          @dragend="onDragEnd"-->
<!--        >-->
<!--          {{ item }}-->
<!--        </div>-->
      </div>
    </div>
  </div>
  {{ numCols }}
  {{ projectImages?.getNumImages() }}
  {{ Object.keys(JSON.parse(JSON.stringify(projectImages?.imagesById))) }}
  {{ Object.keys(projectImages ? instanceToPlain(projectImages?.imagesById) : {}) }}
</template>
<script setup lang="ts">
import { computed, ref } from 'vue';
import { ProjectImagesPayload } from 'src/types/common';
import { instanceToPlain } from 'class-transformer';

const projectImages = defineModel<ProjectImagesPayload>()
const numCols = computed(() => projectImages.value ? projectImages.value.maxColumn() + 1 : 0)
const numRows = computed(() => projectImages.value ? projectImages.value.groupRows.length : 0)

const isDragging = ref(false);
const dragSlot = ref<string | null>(null);
const dragOverSlot = ref<string | null>(null);

const onDragStart = (slot: string) => {
  dragSlot.value = index;
  isDragging.value = true;
};

const onDragEnd = () => {
  dragSlot.value = null;
  dragOverSlot.value = null;
  isDragging.value = false;
};

const onDragOver = (slot: string) => {
  if (dragSlot.value !== slot) {
    dragOverSlot.value = slot;
  }
};

const onDragEnter = (slot: string) => {
  dragOverSlot.value = index;
};

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const onDragLeave = (slot: string) => {
  dragOverSlot.value = null;
};

const onDrop = (slot: string) => {
  if (dragSlot.value === null || dragSlot.value === index) {
    return;
  }

  // Swap items
  const draggedItem = gridItems.value[dragSlot.value];
  gridItems.value[dragSlot.value] = gridItems.value[index];
  gridItems.value[index] = draggedItem;

  onDragEnd();
};

</script>
<style scoped lang="scss">

$grid-item-size: 10rem;

.grid-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.grid-row {
  background: green;
  height: $grid-item-size;
}

.grid-row-unsorted {
  background: blue;
}

.grid-slot {
  width: $grid-item-size;
  height: $grid-item-size;
  border: 2px dashed #ccc;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.grid-slot.dragging-over {
  border-color: #42a5f5;
  background-color: #e3f2fd;
}

.draggable-item {
  width: 80px;
  height: 80px;
  background-color: #42a5f5;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
}

</style>
