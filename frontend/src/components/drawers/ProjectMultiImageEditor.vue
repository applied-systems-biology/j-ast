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
      <q-btn :disable="hasTaskRunning" align="left" class="w-100" color="primary" icon="fa-solid fa-vial-virus"
             label="Set experiment" @click="setAllStringMetadata('Experiment', 'experiment')"/>
      <q-btn :disable="hasTaskRunning" align="left" class="w-100" color="primary" icon="fa-solid fa-flask"
             label="Set sample" @click="setAllStringMetadata('Sample', 'sample')"/>
      <q-btn :disable="hasTaskRunning" align="left" class="w-100" color="primary" icon="fa-solid fa-clock"
             label="Set time point" @click="setAllStringMetadata('Time point', 'timePoint')"/>
      <q-btn :disable="hasTaskRunning" align="left" class="w-100" color="primary" icon="fa-solid fa-gear"
             label="Set assay type" @click="setAllAssayType"/>
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
import {useQuasar} from "quasar";
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
        { label: 'Unknown', value: AssayType.Unknown },
        { label: 'DDA', value: AssayType.DDA },
        { label: 'E-Test', value: AssayType.ETest }
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

</script>
<style lang="scss" scoped>

</style>
