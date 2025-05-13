<template>
  <q-layout view="hHh lpR fFf">
    <q-header>
      <q-toolbar>
        <q-toolbar-title class="row items-center q-gutter-sm">
          <HeaderLogoButtonComponent />
          <div>/</div>
          <q-skeleton v-if="!projectName" type="text" style="width: 200px" />
          <div v-else>{{ projectName }}</div>
          <q-btn
            color="blue"
            @click="goToProject"
            label="Project editor"
            no-caps
            icon="fa-solid fa-arrow-left"
          />
        </q-toolbar-title>
        <AuthManagerComponent />
        <DocumentationComponent />
      </q-toolbar>
      <!--      <q-toolbar class="bg-primary text-white edit-toolbar">-->
      <!--        <q-btn-dropdown color="green" icon="download" label="Download">-->
      <!--          <q-list>-->
      <!--            <q-item v-if="selectedImageId > 0" clickable v-close-popup @click="downloadZip([selectedImageId])">-->
      <!--              <q-item-section>-->
      <!--                <q-item-label>Download selected images and annotations (*.zip)</q-item-label>-->
      <!--              </q-item-section>-->
      <!--            </q-item>-->
      <!--            <q-item v-if="selectedImageId > 0" clickable v-close-popup @click="downloadSelectedImage">-->
      <!--              <q-item-section>-->
      <!--                <q-item-label>Download selected raw images (*.png)</q-item-label>-->
      <!--              </q-item-section>-->
      <!--            </q-item>-->
      <!--            <q-separator v-if="selectedImageId > 0"/>-->
      <!--            <q-item clickable v-close-popup @click="downloadZip(null)">-->
      <!--              <q-item-section>-->
      <!--                <q-item-label>Download everything (*.zip)</q-item-label>-->
      <!--              </q-item-section>-->
      <!--            </q-item>-->
      <!--          </q-list>-->
      <!--        </q-btn-dropdown>-->
      <!--&lt;!&ndash;        <q-btn&ndash;&gt;-->
      <!--&lt;!&ndash;          icon="delete"&ndash;&gt;-->
      <!--&lt;!&ndash;          color="red-4"&ndash;&gt;-->
      <!--&lt;!&ndash;          v-if="selectedImageIds.length > 0"&ndash;&gt;-->
      <!--&lt;!&ndash;          @click="deleteCurrentImage"&ndash;&gt;-->
      <!--&lt;!&ndash;          :disable="hasTaskRunning"&ndash;&gt;-->
      <!--&lt;!&ndash;        >&ndash;&gt;-->
      <!--&lt;!&ndash;          <q-tooltip> Deletes the selected image(s)</q-tooltip>&ndash;&gt;-->
      <!--&lt;!&ndash;        </q-btn>&ndash;&gt;-->
      <!--        <q-btn-->
      <!--          icon="refresh"-->
      <!--          color="primary"-->
      <!--          @click="queryBackend"-->
      <!--          :disable="hasTaskRunning"-->
      <!--        >-->
      <!--          <q-tooltip>Reloads the view</q-tooltip>-->
      <!--        </q-btn>-->
      <!--        <div class="col-grow" />-->
      <!--        <ProjectResultsButton-->
      <!--          :project-id="projectId"-->
      <!--          v-model="resultList"/>-->
      <!--        <ProjectBackendTaskButton-->
      <!--          :project-id="projectId"-->
      <!--          v-model="projectBackendTasks"-->
      <!--          @on-task-finished="onTaskFinished"-->
      <!--        />-->
      <!--      </q-toolbar>-->
    </q-header>
    <q-drawer side="left" bordered :model-value="true">
      <ProjectImageButtonFlat
        :current-image="projectImages.getImageById(imageId)"
        :selected-image-ids="[selectedImageId]"
        v-for="imageId in projectImages.imageIds"
        :key="imageId"
        @image-selected="onImageSelected"
      />
    </q-drawer>
    <q-drawer
      side="right"
      bordered
      :model-value="true"
      class="q-pa-md q-gutter-sm properties-panel"
    >
      <ProjectImageEditor
        v-if="selectedImageId > 0"
        v-model="selectedImage"
        v-model:project-backend-tasks="projectBackendTasks"
      />
    </q-drawer>
    <q-page-container>
      <q-page class="flex">
        <ImageViewer
          class="viewer"
          v-if="selectedImageId > 0"
          :image-backend-url="`/image/${selectedImageId}/raw`"
          :filename="selectedImage.fileName"
        />
        <BackendTaskProgressOverlay
          v-model:backend-tasks="projectBackendTasks"
        />
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import AuthManagerComponent from 'components/layout/AuthManagerComponent.vue';
import HeaderLogoButtonComponent from 'components/layout/HeaderLogoButtonComponent.vue';
import { computed, onMounted, ref, Ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from 'boot/axios';
import { loadPayloadInstanceFromApi } from 'src/types/common';
import ProjectImageEditor from 'components/drawers/ProjectImageEditor.vue';
import { plainToInstance } from 'class-transformer';
import { ProjectMetadataPayload } from 'src/types/project';
import { ProjectImagesPayload } from 'src/types/projectImages';
import {
  BackendTaskPayload,
  BackendTaskTypePayload,
} from 'src/types/backendTasks';
import { useIntervalFn } from '@vueuse/core';
import { ResultPayload } from 'src/types/results';
import BackendTaskProgressOverlay from 'components/backendProcessors/BackendTaskProgressOverlay.vue';
import DocumentationComponent from 'components/layout/DocumentationComponent.vue';
import ProjectImageButtonFlat from 'components/arranger/ProjectImageButtonFlat.vue';
import ImageViewer from 'components/dataViewers/ImageViewer.vue';

function createDummyBackendTask() {
  const task = new BackendTaskPayload();
  task.name = 'Waiting for server ...';
  return task;
}

const $route = useRoute();
const router = useRouter();
const projectName = computed(() => projectPayload.value?.name ?? undefined);
const projectId = $route.params.id + '';
const projectPayload: Ref<ProjectMetadataPayload> = ref(
  new ProjectMetadataPayload()
);
const projectImages = ref<ProjectImagesPayload>(new ProjectImagesPayload());
const selectedImageId = ref<number>(-1);
const selectedImage = computed(() =>
  projectImages.value.getImageById(selectedImageId.value)
);
const availableBackendTasks = ref<Array<BackendTaskTypePayload>>([]);
const projectBackendTasks = ref<Array<BackendTaskPayload>>([
  createDummyBackendTask(),
]);
const resultList = ref<ResultPayload[]>();

// const hasTaskRunning = computed(() => {
//   if (projectBackendTasks.value) {
//     for (const task of projectBackendTasks.value) {
//       if (task.isRunning()) {
//         return true
//       }
//     }
//     return false
//   }
//   return true; // Waiting still for info
// });

defineOptions({
  name: 'ProjectLayout',
});

function goToProject() {
  router.push(`/project/${projectId}`);
}

// function downloadSelectedImage() {
//   downloadFromApi(
//     `/image/${selectedImageId.value}/raw`,
//     ensureExtension(projectImages.value.getImageById(selectedImageId.value).fileName, ['.png'])
//   );
// }
//
// function downloadZip(imageIds : Array<number> | null) {
//   if(imageIds == null) {
//     imageIds = projectImages.value.imageIds
//   }
//
//   // Map to images
//   const items = imageIds.map(id => projectImages.value.getImageById(id));
//
//   // Create Zip items
//   const zipItems : Array<ZipItem>  = []
//   const usedFileNames = new Set<string>()
//   let downloadSizeBytes = 0
//   for(const item of items) {
//     let fileName = item.fileName || `${item.assayType}_${item.experiment}_${item.sample}_${item.timePoint}`
//     fileName = removeExtensionIfPresent(fileName)
//     if(usedFileNames.has(fileName)) {
//       fileName = fileName + "_" + item.id
//     }
//     usedFileNames.add(fileName)
//     zipItems.push({ entryName: ensureExtension(fileName), url: `/image/${item.id}/raw`, content: null })
//     downloadSizeBytes += item.size
//
//     // Add annotations
//     for(const annotation of item.maskImageAnnotations) {
//       zipItems.push({ entryName: ensureExtension(fileName + "_" + annotation.annotationTypeId), url: `/mask-image-annotation/${item.id}/${annotation.annotationTypeId}/raw`, content: null })
//       downloadSizeBytes += annotation.size
//     }
//   }
//
//   $q.dialog({
//     title: 'Download inputs',
//     message: `You are about to download ${zipItems.length} files (${formatFileSize(downloadSizeBytes)}).<br/>Do you want to continue?<br/><br/>Please note that due how the ZIP file is created, your computer needs at least ${formatFileSize(downloadSizeBytes)} of free RAM space.`,
//     html: true,
//     cancel: true,
//     persistent: true
//   }).onOk(() => {
//     const shouldCancel = ref<boolean>(false);
//     const dialog = $q.dialog({
//       title: 'Downloading results ...',
//       message: 'Preparing ...',
//       progress: {
//         spinner: QSpinnerHourglass,
//       },
//       persistent: true,
//       ok: false,
//       cancel: true,
//     })
//     dialog.onCancel(() => {
//       shouldCancel.value = true
//       dialog.hide()
//     })
//
//     generateAndDownloadZip(zipItems, projectPayload.value.name, (percentage, info) => {
//       dialog.update({
//         message: `${percentage}% ${info}`
//       })
//     }, () => shouldCancel.value)
//       .finally(() => {
//         dialog.hide()
//       })
//
//   })
// }

// function onTaskFinished() {
//   // For now just query the backend again
//   queryBackend();
// }

function onImageSelected(imageId: number) {
  selectedImageId.value = imageId;
}

function queryBackend() {
  api.get<ProjectMetadataPayload>(`/project/${projectId}`).then((response) => {
    projectPayload.value = plainToInstance(
      ProjectMetadataPayload,
      response.data
    );
  });
  api
    .get<ProjectImagesPayload>(`/project/${projectId}/images`)
    .then((response) => {
      let payload = plainToInstance(ProjectImagesPayload, response.data);
      payload.fixRowReferences();
      projectImages.value = payload;

      // Auto-select first image
      if (
        selectedImageId.value <= 0 &&
        projectImages.value.imageIds.length > 0
      ) {
        selectedImageId.value = projectImages.value.imageIds[0];
      }
    });
  api.get<BackendTaskTypePayload[]>(`/task/list-types`).then((response) => {
    availableBackendTasks.value = plainToInstance(
      BackendTaskTypePayload,
      response.data
    );
  });
}

function queryTaskBackend() {
  loadPayloadInstanceFromApi(
    `/project/${projectId}/tasks`,
    BackendTaskPayload,
    projectBackendTasks
  );
}

function queryResultListBackend() {
  loadPayloadInstanceFromApi(
    `/project/${projectId}/list-results`,
    Array<ResultPayload>,
    resultList
  );
}

onMounted(() => {
  queryBackend();
  queryTaskBackend();
  queryResultListBackend();
});

useIntervalFn(queryTaskBackend, 2500);
useIntervalFn(queryResultListBackend, 4000);
</script>
<style lang="scss">
.viewer {
  flex-grow: 1;
  width: 100%;
  height: 100%;
  overflow: hidden;
  border-top: 1px solid $blue-5;
}
</style>