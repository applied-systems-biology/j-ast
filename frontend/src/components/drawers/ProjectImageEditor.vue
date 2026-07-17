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
  <q-tabs
      v-model="propertiesPanelTab"
      :active-color="propertiesPanelTab == 'process' ? 'purple' : 'primary'"
      :indicator-color="propertiesPanelTab == 'process' ? 'purple' : 'primary'"
      align="justify"
  >
    <q-tab icon="edit" label="Edit" name="edit"/>
    <q-tab icon="fa-solid fa-gear" label="Process" name="process"/>
  </q-tabs>
  <q-tab-panels v-model="propertiesPanelTab" animated class="grow">
    <q-tab-panel class="q-gutter-sm" name="edit">
      <q-input
          :disable="hasTaskRunning"
          :model-value="model?.fileName"
          class="w-100"
          filled
          label="File name"
          @update:model-value="onUpdateFileName"
      />
      <q-input
          :disable="hasTaskRunning"
          :model-value="model?.experiment"
          class="w-100"
          filled
          label="Experiment"
          @update:model-value="onUpdateExperiment"
      />
      <q-input
          :disable="hasTaskRunning"
          :model-value="model?.sample"
          class="w-100"
          filled
          label="Sample"
          @update:model-value="onUpdateSample"
      />
      <q-input
          :disable="hasTaskRunning"
          :model-value="model?.timePoint"
          class="w-100"
          filled
          label="Time point"
          @update:model-value="onUpdateTimePoint"
      />
      <q-select
          :disable="hasTaskRunning"
          :model-value="model?.assayType"
          :options="['DDA', 'ETest', 'Unknown']"
          class="w-100"
          filled
          label="Assay type"
          @update:model-value="onUpdateAssayType"
      />
      <q-separator/>
      <q-input
          :disable="hasTaskRunning"
          :model-value="model?.pixelSizeMillimeter"
          class="w-100"
          filled
          label="Pixel size (mm)"
          type="number"
          @update:model-value="onUpdatePixelSize"
      />
      <q-input
          v-if="model?.assayType == 'ETest'"
          :disable="hasTaskRunning"
          :model-value="model?.mic"
          class="w-100"
          filled
          label="MIC"
          type="number"
          @update:model-value="onUpdateMIC"
      />
      <q-separator/>
      <MaskImageAnnotationButton
          v-model="model"
          :disable="hasTaskRunning"
          annotation-type-id="plate"
          class="w-100"
      />
      <MaskImageAnnotationButton
          v-if="model?.assayType != 'Unknown'"
          v-model="model"
          :disable="hasTaskRunning"
          annotation-type-id="strip-disk"
          class="w-100"
      />
      <MaskImageAnnotationButton
          v-if="model && imageSupportsMaskAnnotation(model, 'zoi-shape')"
          v-model="model"
          :disable="hasTaskRunning"
          annotation-type-id="zoi-shape"
          class="w-100"
      />
      <StripPresetAnnotationButton
          v-if="model && imageSupportsMetadata(model, 'stripPreset')"
          v-model="model"
          :disable="hasTaskRunning"
          class="w-100"
      />
      <q-separator/>
      <div class="text-bold q-mt-sm">Custom metadata</div>
      <template v-for="(entry, index) in customMetadataEntries" :key="index">
        <q-card  class="w-100">
          <q-card-section class="flex q-gutter-sm" >
            <q-input
                v-model="entry.key"
                :disable="hasTaskRunning"
                :rules="[v => !!v || 'Required', v => !isReservedMetadataKey(v) || 'Reserved key']"
                class="col"
                dense
                filled
                label="Key"
                @update:model-value="onCustomMetadataKeyChange(index)"
            />
            <q-input
                v-model="entry.value"
                :disable="hasTaskRunning"
                class="col"
                dense
                filled
                label="Value"
                @update:model-value="onCustomMetadataValueChange(index)"
            />
          </q-card-section>
          <q-separator />
          <q-card-actions>
            <q-btn :disable="hasTaskRunning"
                   dense
                   flat
                   icon="delete"
                   no-caps no-wrap
                   @click="onRemoveCustomMetadata(index)" label="Delete"/>
            <q-space />
            <q-btn
                :color="entry.showBadge ? 'primary' : 'grey'"
                :icon="entry.showBadge ? 'fa-solid fa-eye' : 'fa-solid fa-eye-slash'"
                dense
                flat
                size="xs"
                @click="onToggleBadge(index)"
            >
              <q-tooltip>Show as badge</q-tooltip>
            </q-btn>
          </q-card-actions>
        </q-card>
      </template>
      <q-btn :disable="hasTaskRunning" icon="fa-solid fa-plus" label="Add custom metadata" no-caps no-wrap
             @click="onAddCustomMetadata"/>
      <div style="height: 32px"></div>
    </q-tab-panel>
    <q-tab-panel class="d-flex-column" name="process">
      <ProjectImageProcessorList v-model:current-view-mode="currentViewMode"
                                 v-model:project-backend-tasks="projectBackendTasks"
                                 v-model:project-images="projectImages"
                                 v-model:selected-image-ids="selectedImageIds"
                                 @on-frontend-task-finished="emitOnFrontendTaskFinished"/>
    </q-tab-panel>
  </q-tab-panels>
</template>
<script lang="ts" setup>
import {debounce} from 'quasar';
import {plainToInstance} from 'class-transformer';
import MaskImageAnnotationButton from 'components/annotationEditors/MaskImageAnnotationButton.vue';
import {sendFailureNotification} from 'src/types/notification';
import {AssayType} from 'src/types/assayType';
import {
  deleteCustomMetadata,
  getCustomMetadataKeys,
  getCustomMetadataValue,
  ImagePayload,
  imageSupportsMaskAnnotation,
  imageSupportsMetadata,
  isReservedMetadataKey,
  setCustomMetadata,
  setCustomMetadataBadgeVisibility
} from 'src/types/image';
import {BackendTaskPayload} from 'src/types/backendTasks';
import {computed, provide, ref, watch} from 'vue';
import StripPresetAnnotationButton from "components/annotationEditors/StripPresetAnnotationButton.vue";
import ProjectImageProcessorList from "components/drawers/ProjectImageProcessorList.vue";
import {ViewMode} from "src/types/view";
import {ProjectImagesPayload} from "src/types/projectImages";
import {FrontEndImageProcessor} from "src/types/frontendTasks";

type SelectValue = string | number | null;

const model = defineModel<ImagePayload>({required: true});
const selectedImageIds = defineModel<Array<number>>("selectedImageIds", {required: true})
const projectImages = defineModel<ProjectImagesPayload>("projectImages", {required: true})
provide('projectImages', projectImages)
const currentViewMode = defineModel<ViewMode>("currentViewMode", {required: true});
const projectBackendTasks = defineModel<BackendTaskPayload[]>('projectBackendTasks', {required: true});
const propertiesPanelTab = defineModel<string>('propertiesPanelTab', {required: true})

const emit = defineEmits<{
  (e: "onFrontendTaskFinished", task: FrontEndImageProcessor): void;
}>();

function emitOnFrontendTaskFinished(task: FrontEndImageProcessor) {
  emit("onFrontendTaskFinished", task);
}

const hasTaskRunning = computed(() => {
  if (projectBackendTasks.value) {
    for (const task of projectBackendTasks.value) {
      if (task.isRunning()) {
        return true
      }
    }
    return false
  }
  return true; // Waiting still for info
});
const uploadToBackend = debounce(uploadToBackend_, 300);

function uploadToBackend_() {
  if (model.value) {
    const payload = plainToInstance(ImagePayload, model.value);
    payload.uploadToBackend().catch(() => {
      sendFailureNotification('Error while updating');
    });
  }
}

function onUpdateFileName(newValue: SelectValue) {
  if (model.value) {
    model.value.fileName = '' + newValue;
    uploadToBackend();
  }
}

function onUpdateExperiment(newValue: SelectValue) {
  if (model.value) {
    model.value.experiment = '' + newValue;
    uploadToBackend();
  }
}

function onUpdateSample(newValue: SelectValue) {
  if (model.value) {
    model.value.sample = '' + newValue;
    uploadToBackend();
  }
}

function onUpdateTimePoint(newValue: SelectValue) {
  if (model.value) {
    model.value.timePoint = '' + newValue;
    uploadToBackend();
  }
}

function onUpdateAssayType(newValue: SelectValue) {
  if (model.value) {
    model.value.assayType = ('' + newValue) as AssayType;
    uploadToBackend();
  }
}

function onUpdatePixelSize(newValue: SelectValue) {
  if (model.value) {
    model.value.pixelSizeMillimeter = Number(newValue);
    uploadToBackend();
  }
}

function onUpdateMIC(newValue: SelectValue) {
  if (model.value) {
    model.value.mic = Number(newValue);
    uploadToBackend();
  }
}

interface CustomMetadataEntry {
  key: string;
  value: string;
  showBadge: boolean;
}

const customMetadataEntries = ref<CustomMetadataEntry[]>([]);
const previousKeys = new Map<number, string>();

// Rebuild entries whenever the selected image changes
watch(() => model.value?.id, () => {
  rebuildCustomMetadataEntries();
}, {immediate: true});

function rebuildCustomMetadataEntries() {
  if (!model.value) {
    customMetadataEntries.value = [];
    previousKeys.clear();
    return;
  }
  const entries: CustomMetadataEntry[] = [];
  previousKeys.clear();
  let idx = 0;
  for (const key of getCustomMetadataKeys(model.value)) {
    const raw = model.value.metadata[key];
    const isObject = raw && typeof raw === 'object' && !Array.isArray(raw);
    entries.push({
      key,
      value: String(getCustomMetadataValue(model.value, key) ?? ''),
      showBadge: isObject ? raw.showBadge === true : false,
    });
    previousKeys.set(idx, key);
    idx++;
  }
  customMetadataEntries.value = entries;
}

function onAddCustomMetadata() {
  customMetadataEntries.value.push({key: '', value: '', showBadge: false});
}

function onCustomMetadataKeyChange(index: number) {
  const entry = customMetadataEntries.value[index];
  if (!entry || !entry.key || isReservedMetadataKey(entry.key)) return;
  if (!model.value) return;
  const oldKey = previousKeys.get(index);
  if (oldKey && oldKey !== entry.key) {
    deleteCustomMetadata(model.value, oldKey);
  }
  setCustomMetadata(model.value, entry.key, entry.value, entry.showBadge);
  previousKeys.set(index, entry.key);
  uploadToBackend();
}

function onCustomMetadataValueChange(index: number) {
  const entry = customMetadataEntries.value[index];
  if (!entry || !entry.key || isReservedMetadataKey(entry.key)) return;
  if (!model.value) return;
  setCustomMetadata(model.value, entry.key, entry.value, entry.showBadge);
  uploadToBackend();
}

function onToggleBadge(index: number) {
  const entry = customMetadataEntries.value[index];
  if (!entry || !entry.key || isReservedMetadataKey(entry.key)) return;
  if (!model.value) return;
  entry.showBadge = !entry.showBadge;
  setCustomMetadataBadgeVisibility(model.value, entry.key, entry.showBadge);
  uploadToBackend();
}

function onRemoveCustomMetadata(index: number) {
  const entry = customMetadataEntries.value[index];
  if (!model.value) return;
  if (entry && entry.key && !isReservedMetadataKey(entry.key)) {
    deleteCustomMetadata(model.value, entry.key);
  }
  customMetadataEntries.value.splice(index, 1);
  previousKeys.delete(index);
  uploadToBackend();
}
</script>
<style scoped></style>
