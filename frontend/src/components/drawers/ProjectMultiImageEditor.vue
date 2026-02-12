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
    <q-tab-panel class="q-gutter-sm grow" name="edit">
      <q-btn no-caps no-wrap :disable="hasTaskRunning" align="left" class="w-100" color="primary" icon="fa-solid fa-vial-virus"
             label="Set experiment" @click="setAllStringMetadata('Experiment', 'experiment')"/>
      <q-btn no-caps no-wrap :disable="hasTaskRunning" align="left" class="w-100" color="primary" icon="fa-solid fa-flask"
             label="Set sample" @click="setAllStringMetadata('Sample', 'sample')"/>
      <q-btn no-caps no-wrap :disable="hasTaskRunning" align="left" class="w-100" color="primary" icon="fa-solid fa-clock"
             label="Set time point" @click="setAllStringMetadata('Time point', 'timePoint')"/>
      <q-btn no-caps no-wrap :disable="hasTaskRunning" align="left" class="w-100" color="primary" icon="fa-solid fa-gear"
             label="Set assay type" @click="setAllAssayType"/>
      <q-separator/>
      <q-btn no-caps no-wrap :disable="hasTaskRunning" align="left" class="w-100" color="primary" icon="fa-solid fa-ruler"
             label="Set pixel size" @click="setAllPixelSize"/>
      <q-btn v-if="hasSelectedETest" no-caps no-wrap :disable="hasTaskRunning" align="left" class="w-100" color="primary" icon="fa-solid fa-ruler-vertical"
             label="Set E-Test strip preset" @click="setAllStripPreset"/>
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
import {Dialog, useQuasar} from "quasar";
import {BackendTaskPayload} from "src/types/backendTasks";
import {computed} from "vue";
import {ImagePayload, setImageMetadata} from "src/types/image";
import {plainToInstance} from "class-transformer";
import {sendFailureNotification} from "src/types/notification";
import ProjectImageProcessorList from "components/drawers/ProjectImageProcessorList.vue";
import {ViewMode} from "src/types/view";
import {ProjectImagesPayload} from "src/types/projectImages";
import {FrontEndImageProcessor} from "src/types/frontendTasks";
import {AssayType} from "src/types/assayType";
import StripPresetSelectorDialog from "components/annotationEditors/StripPresetSelectorDialog.vue";
import {StripPresetPayload} from "src/types/presets";

const selectedImageIds = defineModel<Array<number>>("selectedImageIds", {required: true})
const projectImages = defineModel<ProjectImagesPayload>("projectImages", {required: true})
const projectBackendTasks = defineModel<BackendTaskPayload[]>('projectBackendTasks', {required: true});
const propertiesPanelTab = defineModel<string>('propertiesPanelTab', {required: true})
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
const hasSelectedETest = computed(() => {
  for (const id of selectedImageIds.value) {
    const img = projectImages.value.getImageById(id)
    if (img.assayType == AssayType.ETest) {
      return true
    }
  }
  return false
})
const model = defineModel<ImagePayload[]>({required: true});
const currentViewMode = defineModel<ViewMode>("currentViewMode", {required: true})
const $q = useQuasar();

const emit = defineEmits<{
  (e: "onFrontendTaskFinished", task: FrontEndImageProcessor): void;
}>();

function emitOnFrontendTaskFinished(task: FrontEndImageProcessor) {
  emit("onFrontendTaskFinished", task);
}

function setAllStringMetadata(name: string, key: string) {
  $q.dialog({
    title: 'Set ' + name.toLowerCase(),
    message: 'This will set the ' + name.toLowerCase() + " for all selected " + model.value!.length + " images.",
    prompt: {
      model: '',
      type: 'text',
    },
    cancel: true
  }).onOk((data: string) => {
    for (const image of model.value!) {
      setImageMetadata(image, key, data)
      const payload = plainToInstance(ImagePayload, image);
      payload.uploadToBackend().catch(() => {
        sendFailureNotification('Error while updating');
      });
    }
  });
}

function setAllAssayType() {
  $q.dialog({
    title: 'Set assay type',
    message: "This will set the assay type for all selected " + model.value!.length + " images.",
    options: {
      type: 'radio',
      model: AssayType.Unknown,
      // inline: true
      items: [
        {label: 'Unknown', value: AssayType.Unknown},
        {label: 'DDA', value: AssayType.DDA},
        {label: 'E-Test', value: AssayType.ETest}
      ]
    },
    cancel: true
  }).onOk((data: string) => {
    for (const image of model.value!) {
      setImageMetadata(image, "assayType", data)
      const payload = plainToInstance(ImagePayload, image);
      payload.uploadToBackend().catch(() => {
        sendFailureNotification('Error while updating');
      });
    }
  });
}

function setAllPixelSize() {
  $q.dialog({
    title: 'Set pixel size',
    message: "This will set the pixel size (in millimeters) for all selected " + model.value!.length + " images.",
    prompt: {
      model: '0',
      type: 'number',
    },
    cancel: true
  }).onOk((data: number) => {
    for (const image of model.value!) {
      image.pixelSizeMillimeter = data
      const payload = plainToInstance(ImagePayload, image);
      payload.uploadToBackend().catch(() => {
        sendFailureNotification('Error while updating');
      });
    }
  });
}

function setAllStripPreset() {
  Dialog.create({
    component: StripPresetSelectorDialog,
    componentProps: {
      persistent: true,
    },
  })
      .onOk((payload : StripPresetPayload) => {
        if(payload) {
          for (const image of model.value!) {
            setImageMetadata(image, "stripPreset", payload)
            image.version += 1
            plainToInstance(ImagePayload, image).uploadToBackend().catch(() => {
              sendFailureNotification('Error while updating');
            });
          }
        }
      })
      .onCancel(() => {})
      .onDismiss(() => {});
}

</script>
<style lang="scss" scoped>

</style>
