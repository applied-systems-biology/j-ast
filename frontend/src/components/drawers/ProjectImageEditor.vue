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
import {ImagePayload, imageSupportsMaskAnnotation, imageSupportsMetadata} from 'src/types/image';
import {BackendTaskPayload} from 'src/types/backendTasks';
import {computed} from 'vue';
import StripPresetAnnotationButton from "components/annotationEditors/StripPresetAnnotationButton.vue";
import ProjectImageProcessorList from "components/drawers/ProjectImageProcessorList.vue";
import {ViewMode} from "src/types/view";
import {ProjectImagesPayload} from "src/types/projectImages";
import {FrontEndImageProcessor} from "src/types/frontendTasks";

type SelectValue = string | number | null;

const model = defineModel<ImagePayload>({required: true});
const selectedImageIds = defineModel<Array<number>>("selectedImageIds", {required: true})
const projectImages = defineModel<ProjectImagesPayload>("projectImages", {required: true})
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
</script>
<style scoped></style>
