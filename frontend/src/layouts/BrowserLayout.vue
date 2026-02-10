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
        <UserManagerComponent />
        <DocumentationComponent />
      </q-toolbar>
    </q-header>
    <q-drawer side="left" bordered :model-value="true" class="image-list">
      <q-input v-model="filterText" dense outlined clearable class="q-ma-sm" debounce="1000">
        <template v-slot:prepend>
          <q-icon name="search" />
        </template>
      </q-input>
      <q-scroll-area class="w-100 image-list-items">
        <ProjectImageButtonFlat
          :current-image="projectImages.getImageById(imageId)"
          :selected-image-ids="[selectedImageId]"
          v-for="imageId in filteredImageIds"
          :key="imageId"
          @image-selected="onImageSelected"
        />
      </q-scroll-area>
      <q-toolbar>
        <q-btn icon="fa-solid fa-chevron-left" color="secondary" @click="goToPreviousImage"/>
        <q-space />
        <q-btn icon="fa-solid fa-chevron-right" color="secondary" @click="goToNextImage"/>
      </q-toolbar>
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
import UserManagerComponent from 'components/layout/UserManagerComponent.vue';
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
import { filterAppliesToImage } from 'src/types/filters';

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
const filterText = ref("")
const filteredImageIds = computed(() => {
  if(filterText.value && filterText.value.length > 0) {
    return projectImages.value.imageIds.filter(index => {
      const image = projectImages.value.getImageById(index);
      if(image) {
        return filterAppliesToImage(filterText.value, image)
      }
      return false
    })
  }
  else {
    return projectImages.value.imageIds
  }
})

defineOptions({
  name: 'ProjectLayout',
});

function goToPreviousImage() {
  const index = filteredImageIds.value.indexOf(selectedImageId.value);
  if (index > -1 && filteredImageIds.value.length > 0) {
    if(index > 0) {
      selectedImageId.value = filteredImageIds.value[index - 1];
    }
    else {
      selectedImageId.value = filteredImageIds.value[filteredImageIds.value.length - 1];
    }
  }
}

function goToNextImage() {
  const index = filteredImageIds.value.indexOf(selectedImageId.value);
  if (index > -1 && filteredImageIds.value.length > 0) {
    if(index < filteredImageIds.value.length - 1) {
      selectedImageId.value = filteredImageIds.value[index + 1];
    }
    else {
      selectedImageId.value = filteredImageIds.value[0];
    }
  }
}

function goToProject() {
  router.push(`/project/${projectId}`);
}

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

.image-list {
  display: flex;
  flex-direction: column;
}

.image-list-items {
  flex-grow: 1;
}
</style>