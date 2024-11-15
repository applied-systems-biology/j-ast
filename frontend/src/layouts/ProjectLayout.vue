<template>
  <q-layout view="hHh lpR fFf">
    <q-header>
      <q-toolbar>
        <q-toolbar-title class="row items-center q-gutter-sm">
          <HeaderLogoButtonComponent />
          <div>/</div>
          <q-skeleton v-if="!projectName" type="text" style="width: 200px" />
          <div v-else>{{ projectName }}</div>
          <q-btn-group flat>
            <q-btn flat @click="editProjectName">
              <q-icon name="edit" />
            </q-btn>
            <q-btn flat @click="deleteProject">
              <q-icon name="delete" />
            </q-btn>
          </q-btn-group>
        </q-toolbar-title>
        <LoginButtonComponent />
      </q-toolbar>
      <q-toolbar class="bg-primary text-white">
        <ToggleButton
          not-selected-icon="upload"
          selected-icon="close"
          class="bg-secondary q-mr-sm"
          v-model="drawerLeft"
          >Upload
        </ToggleButton>
        <ToggleButton
          selected-icon="close"
          not-selected-icon="sort"
          class="bg-secondary q-mr-sm"
          v-model="drawerUnsortedImages">
          <span v-if="projectImages?.unsortedRow.images.length" class="text-bold flex flex-center">Unsorted images ({{ projectImages?.unsortedRow.images.length || 0 }})</span>
          <span v-else class="text-bold flex flex-center">Unsorted images</span>
        </ToggleButton>
        <q-btn
          icon="delete"
          color="red-5"
          v-if="selectedImageId >= 0"
          @click="deleteSelectedImage"
          >Delete</q-btn
        >
      </q-toolbar>
    </q-header>
    <q-drawer elevated side="left" bordered v-model="drawerLeft">
      <ImageUploaderComponent
        :project-id="projectId[0]"
        @finished="reloadProjectInfo"
      />
    </q-drawer>
    <q-drawer
      elevated
      side="right"
      bordered
      v-model="drawerRight"
      class="q-pa-sm q-gutter-sm"
    >
      <ProjectImageEditor v-model="selectedImage" />
    </q-drawer>
    <q-page-container>
      <q-page class="flex column q-gutter-sm">
        <ImageArrangerComponent v-model="projectImages" @selected-image-changed="onSelectedImageChanged" :show-unsorted="drawerUnsortedImages"/>
      </q-page>
    </q-page-container>
    <q-footer>
      <ImprintComponent />
    </q-footer>
  </q-layout>
</template>

<script setup lang="ts">
import ImprintComponent from 'components/ImprintComponent.vue';
import LoginButtonComponent from 'components/AuthManagerComponent.vue';
import HeaderLogoButtonComponent from 'components/HeaderLogoButtonComponent.vue';
import { onMounted, Ref, ref, computed } from 'vue';
import ToggleButton from 'components/ToggleButton.vue';
import ImageUploaderComponent from 'components/ImageUploaderComponent.vue';
import { useQuasar } from 'quasar';
// import { VueDraggableNext as draggable } from 'vue-draggable-next';
import { useRoute, useRouter } from 'vue-router';
import { api } from 'boot/axios';
import {
  CreateEditProjectRequest,
  ProjectImagesPayload,
  ProjectMetadataPayload,
} from 'src/types/common';
import ProjectImageEditor from 'components/ProjectImageEditor.vue';
import { plainToInstance } from 'class-transformer';
import ImageArrangerComponent from 'components/ImageArrangerComponent.vue';

const $q = useQuasar();
const $route = useRoute();
const router = useRouter();
const drawerLeft: Ref<boolean> = ref(false);
const drawerUnsortedImages = ref(true)
const projectName = computed(() => projectInfo.value?.name ?? undefined);
const projectId = $route.params.id;
const projectInfo: Ref<ProjectMetadataPayload> = ref(
  new ProjectMetadataPayload()
);
const projectImages = ref<ProjectImagesPayload>(new ProjectImagesPayload());
const selectedImageId = ref<number>(-1);

// Computed values
const drawerRight = computed(() => selectedImageId.value >= 0);
const selectedImage = computed(() =>
  projectImages.value.getImageById(selectedImageId.value)
);

defineOptions({
  name: 'ProjectLayout',
});

function editProjectName() {
  $q.dialog({
    title: 'Edit project name',
    message: 'Please enter a new project name',
    prompt: {
      model: projectName.value || '',
      type: 'text',
    },
    cancel: true,
    persistent: true,
  }).onOk((data: string) => {
    api
      .post<CreateEditProjectRequest>(`/project/${projectId}/edit`, {
        name: data,
      } as CreateEditProjectRequest)
      .then((response) => {
        projectInfo.value = plainToInstance(
          ProjectMetadataPayload,
          response.data
        );
      });
  });
}

function deleteProject() {
  $q.dialog({
    title: 'Delete project',
    message: 'Do your really want to delete the current project?',
    cancel: {
      label: 'No',
    },
    ok: {
      label: 'Yes',
      color: 'red',
    },
    persistent: true,
  }).onOk(() => {
    api.post(`/project/${projectId}/delete`, {}).then(() => {
      router.push('/');
    });
  });
}

function deleteSelectedImage() {
  if (selectedImage.value) {
    $q.dialog({
      title: 'Delete image',
      message: `Do your really want to delete the image '${selectedImage.value.fileName}'?`,
      cancel: {
        label: 'No',
      },
      ok: {
        label: 'Yes',
        color: 'red',
      },
      persistent: true,
    }).onOk(() => {
      api.post(`/image/${selectedImageId.value}/delete`, {}).then(() => {
        reloadProjectInfo();
      });
    });
  }
}

function reloadProjectInfo() {
  api.get<ProjectMetadataPayload>(`/project/${projectId}`).then((response) => {
    projectInfo.value = plainToInstance(ProjectMetadataPayload, response.data);
  });
  api
    .get<ProjectImagesPayload>(`/project/${projectId}/images`)
    .then((response) => {
      let payload = plainToInstance(ProjectImagesPayload, response.data);
      console.log(response.data)
      console.log(payload)
      payload.fixRowReferences();
      projectImages.value = payload;

      // Un-select the image
      if (!selectedImage.value) {
        selectedImageId.value = -1;
      }
    });
}

function onSelectedImageChanged(newImageId : number) {
  selectedImageId.value = newImageId;
}

onMounted(() => {
  reloadProjectInfo();
});
</script>
<style scoped>
</style>
