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
      <q-toolbar class="bg-primary text-white edit-toolbar">
        <ToggleButton
          not-selected-icon="upload"
          selected-icon="close"
          class="bg-secondary"
          v-model="drawerLeft">
          Upload
          <q-tooltip>
            Allows you to upload raw image files.
            Please note that all new images will be put into the "Unsorted images" list.
          </q-tooltip>
        </ToggleButton>
        <ToggleButton
          selected-icon="close"
          not-selected-icon="sort"
          class="bg-secondary"
          v-model="drawerUnsortedImages"
        >
          <span
            v-if="projectImages?.unsortedRow.images.length"
            class="text-bold flex flex-center"
            >Unsorted images ({{
              projectImages?.unsortedRow.images.length || 0
            }})</span
          >
          <span v-else class="text-bold flex flex-center">Unsorted images</span>
          <q-tooltip>All images that have not yet been organized are stored here.</q-tooltip>
        </ToggleButton>
        <q-btn
          icon="deselect"
          color="blue"
          v-if="selectedImageIds.length > 0"
          @click="selectedImageIds = []"
          ><q-tooltip>
          Clears the current selection
        </q-tooltip>
        </q-btn>
        <q-btn
          icon="download"
          color="blue"
          v-if="selectedImageIds.length > 0"
          @click="downloadSelectedImages"
          >
          <q-tooltip>
            Downloads the selected image(s)
          </q-tooltip>
        </q-btn>
        <q-btn
          icon="delete"
          color="red-4"
          v-if="selectedImageIds.length > 0"
          @click="deleteSelectedImages"
        >
          <q-tooltip>
            Deletes the selected image(s)
          </q-tooltip>
        </q-btn>
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
      <div class="row reverse">
        <q-btn
          icon="close"
          flat
          padding="none"
          @click="selectedImageIds = []"
        />
      </div>
      <ProjectImageEditor v-model="selectedImage" />
    </q-drawer>
    <q-page-container>
      <q-page class="flex column q-gutter-sm">
        <ImageArrangerComponent
          v-model="projectImages"
          v-model:selectedImageIds="selectedImageIds"
          :show-unsorted="drawerUnsortedImages"
        />
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
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
  CreateEditProjectRequest, downloadFromApi, ensureExtension,
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
const drawerUnsortedImages = ref(false);
const projectName = computed(() => projectPayload.value?.name ?? undefined);
const projectId = $route.params.id;
const projectPayload: Ref<ProjectMetadataPayload> = ref(
  new ProjectMetadataPayload()
);
const projectImages = ref<ProjectImagesPayload>(new ProjectImagesPayload());
const selectedImageIds = ref<Array<number>>([]);

// Computed values
const drawerRight = computed(() => selectedImageIds.value.length > 0);
const selectedImage = computed(() =>
  projectImages.value.getImageById(
    selectedImageIds.value.length > 0
      ? selectedImageIds.value[selectedImageIds.value.length - 1]
      : -1
  )
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
        projectPayload.value = plainToInstance(
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

function deleteSelectedImages() {
  if (selectedImageIds.value.length > 0) {
    $q.dialog({
      title: 'Delete images',
      message: `Do your really want to delete the selected images?`,
      cancel: {
        label: 'No',
      },
      ok: {
        label: 'Yes',
        color: 'red',
      },
      persistent: true,
    }).onOk(() => {
      const promises = [];
      for (const id of selectedImageIds.value) {
        promises.push(api.post(`/image/${id}/delete`));
      }
      Promise.all(promises).then(() => {
        reloadProjectInfo();
      });
    });
  }
}

function downloadSelectedImages() {
  for (const id of selectedImageIds.value) {
    downloadFromApi(`/image/${id}/raw`, ensureExtension(projectImages.value.getImageById(id).fileName, [".png"]))
  }
}

function reloadProjectInfo() {
  api.get<ProjectMetadataPayload>(`/project/${projectId}`).then((response) => {
    projectPayload.value = plainToInstance(ProjectMetadataPayload, response.data);
  });
  api
    .get<ProjectImagesPayload>(`/project/${projectId}/images`)
    .then((response) => {
      let payload = plainToInstance(ProjectImagesPayload, response.data);
      // console.log(response.data);
      // console.log(payload);
      payload.fixRowReferences();
      projectImages.value = payload;

      // Un-select the images
      selectedImageIds.value = [];

      // Set up the unsorted images drawer
      drawerUnsortedImages.value =
        projectImages.value.unsortedRow.images.length > 0;
    });
}

onMounted(() => {
  reloadProjectInfo();
});
</script>
<style scoped lang="scss">
.edit-toolbar > * {
  margin-right: 10px;
}
</style>
