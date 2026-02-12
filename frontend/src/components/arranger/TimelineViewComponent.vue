<template>
  <div class="grid-container" @click="selectedImageIds = []">
    <q-card v-if="showUnsorted" class="full-width bg-indigo-1" flat>
      <q-card-section class="flex row q-gutter-sm">
        <div class="text-bold flex flex-center">Unsorted images</div>
      </q-card-section>

      <!-- Unsorted -->
      <q-card-section>
        <q-scroll-area class="unsorted-scroll-area" visible>
          <div class="grid-row grid-row-unsorted bg-indigo-1">
            <div
                v-for="(k, columnIndex) in Math.min(
                maxNumRenderedUnsorted,
                numUnsorted
              ) + 1"
                :key="`col-${columnIndex}`"
                class="grid-slot"
                @drop="onDrop(new SlotIndex(-1, columnIndex))"
                @dragover.prevent="onDragOver(new SlotIndex(-1, columnIndex))"
                @dragenter.prevent="onDragEnter(new SlotIndex(-1, columnIndex))"
                @dragleave.prevent="onDragLeave(new SlotIndex(-1, columnIndex))"
            >
              <ProjectImageButton
                  v-if="getUnsortedImage(columnIndex)"
                  :current-image="getUnsortedImage(columnIndex)!"
                  :disabled="hasTaskRunning"
                  :selected-image-ids="selectedImageIds"
                  class="draggable-item"
                  draggable="true"
                  @dragend="onDragEnd"
                  @dragstart="onDragStart(new SlotIndex(-1, columnIndex))"
                  @image-selected="onImageSelected"
              />
            </div>
            <q-btn
                class="q-ma-sm load-more-unsorted" flat
                icon="refresh"
                no-caps
                no-wrap
                @click="maxNumRenderedUnsorted += 5"
            >
              <div class="text-uppercase">
                Show more items ({{
                  Math.max(
                      0,
                      numUnsorted - Math.min(maxNumRenderedUnsorted, numUnsorted)
                  )
                }}
                left)
              </div>
              <div class="text-caption">
                Items may be hidden for performance reasons
              </div>
            </q-btn>
          </div>
        </q-scroll-area>
      </q-card-section>

      <q-separator/>

      <q-card-actions>
        <q-btn
            color="secondary" icon="select_all"
            label="Select all unsorted"
            no-caps
            no-wrap
            @click.stop="selectAllUnsorted"
        />
        <q-btn
            color="green" icon="fa-solid fa-wand-magic-sparkles"
            label="All-in-one preparation"
            no-caps
            no-wrap
            @click.stop="quickAllInOnePreparation"
        >
          <q-tooltip>
            Automatically attempts to fill in metadata, sort images, and find
            annotations.
          </q-tooltip>
        </q-btn>
      </q-card-actions>

      <q-separator/>
    </q-card>

    <!-- Sorted -->
    <q-infinite-scroll class="table-scroll-area q-mt-md" @load="loadNextRows">
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
              <q-icon :name="badge.icon"/>
              <span class="q-ml-sm">{{ badge.text }}</span>
            </q-badge>
          </q-scroll-area>
          <q-btn
              flat icon="fa-solid fa-chevron-down"
              no-caps
              no-wrap
              size="xs"
              @click.stop="selectColumn(columnIndex, $event)"
          >
            <q-tooltip>Select the whole column</q-tooltip>
          </q-btn>
        </div>
      </div>
      <template
          v-for="(i, rowIndex) in numRenderedRows + 1"
          :key="`row-${rowIndex}`"
      >
        <div v-if="filterAppliesToRow(rowIndex)" class="grid-row bg-indigo-1">
          <div class="grid-row-label">
            <q-scroll-area class="row-label-scroll-area">
              <q-badge
                  v-for="badge in getRowBadges(rowIndex)"
                  :key="`${badge.type}-${badge.text}`"
                  :style="{ backgroundColor: badge.color }"
              >
                <q-icon :name="badge.icon"/>
                <span class="q-ml-sm">{{ badge.text }}</span>
              </q-badge>
            </q-scroll-area>
            <q-btn
                class="select-all-button" flat
                icon="fa-solid fa-chevron-right"
                no-caps
                no-wrap
                size="xs"
                @click.stop="selectRow(rowIndex, $event)"
            >
              <q-tooltip>Select the whole row</q-tooltip>
            </q-btn>
          </div>
          <div
              v-for="(j, columnIndex) in numCols + 1"
              :key="`col-${columnIndex}`"
              class="grid-slot"
              @drop="onDrop(new SlotIndex(rowIndex, columnIndex))"
              @dragover.prevent="onDragOver(new SlotIndex(rowIndex, columnIndex))"
              @dragenter.prevent="
              onDragEnter(new SlotIndex(rowIndex, columnIndex))
            "
              @dragleave.prevent="
              onDragLeave(new SlotIndex(rowIndex, columnIndex))
            "
          >
            <ProjectImageButton
                v-if="getImageBySlot(rowIndex, columnIndex)"
                :current-image="getImageBySlot(rowIndex, columnIndex)!"
                :disabled="hasTaskRunning"
                :selected-image-ids="selectedImageIds"
                class="draggable-item"
                draggable="true"
                @dragend="onDragEnd"
                @dragstart="onDragStart(new SlotIndex(rowIndex, columnIndex))"
                @image-selected="onImageSelected"
            />
          </div>
        </div>
      </template>
      <q-btn
          :label="'Load more rows (' + Math.max(0, numRows - numRenderedRows) + ' left)'" class="q-ma-sm"
          flat
          icon="refresh"
          no-caps
          no-wrap
          @click="maxNumRenderedRows += 5"
      />
    </q-infinite-scroll>
  </div>
</template>
<script lang="ts" setup>
import {computed, onMounted, ref, watch} from 'vue';
import ProjectImageButton from 'components/arranger/ProjectImageButton.vue';
import {sendFailureNotification} from 'src/types/notification';
import {Badge} from 'src/types/badge';
import {ProjectImagesPayload, ProjectImagesPayloadRow,} from 'src/types/projectImages';
import {BackendTaskPayload, BackendTaskTypePayload, doBackendTask,} from 'src/types/backendTasks';
import {api} from 'boot/axios';
import {plainToInstance} from 'class-transformer';
import {AssayType} from 'src/types/assayType';

const props = defineProps<{
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

const selectedImageIds = defineModel<Array<number>>('selectedImageIds', {required: true});
const projectBackendTasks = defineModel<BackendTaskPayload[]>('backendTasks', {required: true});
const projectImages = defineModel<ProjectImagesPayload>({required: true});
const filterText = defineModel<string>('filterText', {required: true});

const numCols = computed(() =>
    projectImages.value ? projectImages.value.maxColumn() + 1 : 0
);
const numRows = computed(() =>
    projectImages.value ? projectImages.value.groupRows.length : 0
);
const maxNumRenderedRows = ref(10);
const numRenderedRows = computed(() => {
  return Math.min(numRows.value, maxNumRenderedRows.value);
});

const numUnsorted = computed(() => {
  return projectImages.value?.unsortedRow.images.length || 0;
});
const maxNumRenderedUnsorted = ref(10);

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

const isDragging = ref(false);
const dragSlot = ref<SlotIndex | null>(null);
const dragOverSlot = ref<SlotIndex | null>(null);

function filterAppliesToRow(rowIndex: number) {
  if (filterText.value && filterText.value.length > 0 && projectImages.value) {
    const row = projectImages.value.groupRows[rowIndex];
    if (!row) {
      return true;
    }
    const tags = new Set<string>();
    for (const image of row.images) {
      if (image.experiment) {
        tags.add('experiment:' + image.experiment);
      }
      if (image.sample) {
        tags.add('sample:' + image.sample);
      }
      if (image.assayType != AssayType.Unknown) {
        tags.add('assayType:' + image.assayType);
      }
      tags.add(image.fileName);

      let hasPlate = false;
      let hasDiskStrip = false;
      let hasZOIShape = false;

      for (const annotation of image.maskImageAnnotations) {
        if (annotation.version > 0) {
          if (annotation.annotationTypeId == 'plate') {
            hasPlate = true;
          } else if (annotation.annotationTypeId == 'strip-disk') {
            hasDiskStrip = true;
          } else if (annotation.annotationTypeId == 'zoi-shape') {
            hasZOIShape = true;
          }
        }
      }

      tags.add('hasPlate:' + (hasPlate ? 'yes' : 'no'));
      tags.add('hasDiskStrip:' + (hasDiskStrip ? 'yes' : 'no'));
      if (image.assayType == AssayType.ETest) {
        tags.add('hasZOIShape:' + (hasZOIShape ? 'yes' : 'no'));
      }
    }

    // Tags to string
    const searchString = [...tags].join(' ');
    return searchString.includes(filterText.value);
  } else {
    return true;
  }
}

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
  if (hasTaskRunning.value) {
    sendFailureNotification('No changes are allowed while a task is running');
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
    const newSelection = projectImages.value?.unsortedRow.images
        .filter((img) => img.groupRow < 0)
        .map((img) => img.id);

    selectedImageIds.value = newSelection;
  }
}

function quickAllInOnePreparation() {
  selectAllUnsorted();

  if (selectedImageIds.value.length > 0) {
    if (selectedImageIds.value && projectImages.value) {
      api.get<BackendTaskTypePayload[]>(`/task/list-types`).then((response) => {
        const available = plainToInstance(
            BackendTaskTypePayload,
            response.data
        );
        for (const tool of available) {
          if (tool.taskId == 'aio-prepare' && projectImages.value) {
            // console.log(selectedImageIds.value);
            const selectedImages = selectedImageIds.value.map(
                (id) => projectImages.value!.getImageById(id)!
            );
            // console.log(selectedImages)
            doBackendTask(
                [...selectedImages],
                Number(projectImages.value.projectId),
                tool,
                projectImages.value
            );
            break;
          }
        }
      });
    }
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

function selectAll() {
  const newSelected: Array<number> = []
  if (projectImages.value) {
    if (props.showUnsorted) {
      for (const image of projectImages.value.unsortedRow.images) {
        newSelected.push(image.id);
      }
    }
    for (let rowIndex = 0; rowIndex < projectImages.value.groupRows.length; rowIndex++) {
      if (filterAppliesToRow(rowIndex)) {
        for (const image of projectImages.value.groupRows[rowIndex].images) {
          newSelected.push(image.id);
        }
      }
    }
  }
  selectedImageIds.value = newSelected
}

onMounted(() => {
  watch(projectImages, () => {
    // console.log('new project images:' + projectImages.value?.projectId);
    if (projectImages.value?.projectId != lastProjectId.value) {
      maxNumRenderedRows.value = 10;
    }
  });
});

defineExpose({selectAll})

</script>
<style lang="scss" scoped>
$grid-item-width: 22rem;
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

.load-more-unsorted {
  width: $grid-item-width;
  height: $grid-item-height;
  border: 2px solid #ccc;
}
</style>
