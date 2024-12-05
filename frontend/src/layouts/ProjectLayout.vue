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
        <AuthManagerComponent />
      </q-toolbar>
      <q-toolbar class="bg-primary text-white edit-toolbar">
        <ToggleButton
          not-selected-icon="upload"
          selected-icon="close"
          class="bg-secondary"
          v-model="drawerLeft"
        >
          Upload
          <q-tooltip>
            Allows you to upload raw image files. Please note that all new
            images will be put into the "Unsorted images" list.
          </q-tooltip>
        </ToggleButton>
        <ToggleButton
          selected-icon="close"
          not-selected-icon="sort"
          :class="
            projectImages?.unsortedRow.images.length
              ? 'bg-secondary'
              : 'bg-blue'
          "
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
          <q-tooltip
            >All images that have not yet been organized are stored here.
          </q-tooltip>
        </ToggleButton>
        <q-btn
          icon="select_all"
          color="blue"
          v-if="selectedImageIds.length == 0"
          @click="selectAll"
        >
          <q-tooltip
            >Selects all visible images. To select unsorted images, open the
            "Unsorted images" view
          </q-tooltip>
        </q-btn>
        <q-btn
          icon="deselect"
          color="blue"
          v-if="selectedImageIds.length > 0"
          @click="selectedImageIds = []"
        >
          <q-tooltip> Clears the current selection</q-tooltip>
        </q-btn>
        <q-btn
          icon="download"
          color="blue"
          v-if="selectedImageIds.length > 0"
          @click="downloadSelectedImages"
        >
          <q-tooltip> Downloads the selected image(s)</q-tooltip>
        </q-btn>
        <q-btn
          color="accent"
          label="Process"
          icon="fa-solid fa-gear"
          v-if="selectedImageIds.length > 0"
        >
          <q-menu>
            <q-list style="min-width: 100px">
              <!-- Front-end processors -->
              <q-item
                v-for="tool in frontEndImageProcessors"
                :key="tool.label"
                clickable
                v-close-popup
                @click="doFrontEndProcessor(tool)"
              >
                <q-item-section avatar>
                  <q-icon :name="tool.icon" />
                </q-item-section>
                <q-item-section>{{ tool.label }}</q-item-section>
                <q-tooltip>{{ tool.tooltip }}</q-tooltip>
              </q-item>
              <q-separator />
              <q-item
                v-for="category in availableBackendTasksCategories"
                :key="category"
                clickable
              >
                <q-item-section>{{ category }}</q-item-section>
                <q-item-section side>
                  <q-icon name="keyboard_arrow_right" />
                </q-item-section>

                <q-menu anchor="top end" self="top start">
                  <q-item
                    v-for="tool in availableBackendTasks.filter(
                      (task) => task.category == category
                    )"
                    :key="tool.taskId"
                    clickable
                    v-close-popup
                    @click="doBackendTaskClicked(tool)"
                  >
                    <q-item-section avatar>
                      <q-icon name="fa-solid fa-wand-magic-sparkles" />
                    </q-item-section>
                    <q-item-section>{{ tool.name }}</q-item-section>
                    <q-tooltip>{{ tool.description }}</q-tooltip>
                  </q-item>
                </q-menu>
              </q-item>
              <q-item
                v-for="tool in availableBackendTasks.filter(
                  (task) => !task.category
                )"
                :key="tool.taskId"
                clickable
                v-close-popup
                @click="doBackendTaskClicked(tool)"
              >
                <q-item-section avatar>
                  <q-icon name="fa-solid fa-wand-magic-sparkles" />
                </q-item-section>
                <q-item-section>{{ tool.name }}</q-item-section>
                <q-tooltip>{{ tool.description }}</q-tooltip>
              </q-item>
            </q-list>
          </q-menu>
        </q-btn>
        <q-btn
          icon="delete"
          color="red-4"
          v-if="selectedImageIds.length > 0"
          @click="deleteSelectedImages"
        >
          <q-tooltip> Deletes the selected image(s)</q-tooltip>
        </q-btn>
        <div class="col-grow" />
        <ProjectResultsButton
          :project-id="projectId[0]"
          v-model="resultList"/>
        <ProjectBackendTaskButton
          :project-id="projectId[0]"
          v-model="projectBackendTasks"
          @on-task-finished="onTaskFinished"
        />
      </q-toolbar>
    </q-header>
    <q-drawer elevated side="left" bordered v-model="drawerLeft">
      <ImageUploaderComponent
        :project-id="projectId[0]"
        @finished="queryBackend"
      />
    </q-drawer>
    <q-drawer
      elevated
      side="right"
      bordered
      v-model="drawerRight"
      class="q-pa-md q-gutter-sm properties-panel"
    >
      <div class="row reverse">
        <q-btn
          icon="close"
          flat
          padding="none"
          @click="selectedImageIds = []"
        />
      </div>
      <ProjectMultiImageEditor
        v-if="selectedImageIds.length > 1"
        v-model="selectedImageIds"
      />
      <ProjectImageEditor
        v-model="lastSelectedImage"
        v-model:project-backend-tasks="projectBackendTasks"
      />
    </q-drawer>
    <q-page-container>
      <q-page class="flex column q-gutter-sm">
        <ImageArrangerComponent
          v-model="projectImages"
          v-model:selected-image-ids="selectedImageIds"
          v-model:backend-tasks="projectBackendTasks"
          :show-unsorted="drawerUnsortedImages"
        />
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import AuthManagerComponent from 'components/layout/AuthManagerComponent.vue';
import HeaderLogoButtonComponent from 'components/layout/HeaderLogoButtonComponent.vue';
import { computed, onMounted, ref, Ref } from 'vue';
import ToggleButton from 'components/utils/ToggleButton.vue';
import ImageUploaderComponent from 'components/drawers/ImageUploaderComponent.vue';
import { useQuasar } from 'quasar';
// import { VueDraggableNext as draggable } from 'vue-draggable-next';
import { useRoute, useRouter } from 'vue-router';
import { api } from 'boot/axios';
import { downloadFromApi, ensureExtension, loadPayloadInstanceFromApi } from 'src/types/common';
import ProjectImageEditor from 'components/drawers/ProjectImageEditor.vue';
import { plainToInstance } from 'class-transformer';
import ImageArrangerComponent from 'components/arranger/ImageArrangerComponent.vue';
import ProjectMultiImageEditor from 'components/drawers/ProjectMultiImageEditor.vue';
import {
  FrontEndImageProcessor,
  frontEndImageProcessors,
} from 'src/types/frontendTasks';
import { onDialogYes } from 'src/types/dialog';
import {
  sendFailureNotification,
  sendSuccessNotification,
} from 'src/types/notification';
import {
  CreateEditProjectRequest,
  ProjectMetadataPayload,
} from 'src/types/project';
import { ProjectImagesPayload } from 'src/types/projectImages';
import {
  BackendTaskTypePayload,
  BackendTaskPayload,
  doBackendTask,
} from 'src/types/backendTasks';
import { useIntervalFn } from '@vueuse/core';
import ProjectBackendTaskButton from 'components/layout/ProjectBackendTaskButton.vue';
import ProjectResultsButton from 'components/layout/ProjectResultsButton.vue';
import { ResultPayload } from 'src/types/results';

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
const availableBackendTasks = ref<Array<BackendTaskTypePayload>>([]);
const projectBackendTasks = ref<Array<BackendTaskPayload>>([]);
const resultList = ref<ResultPayload[]>();
const availableBackendTasksCategories = computed(() => {
  const result = new Set<string>();
  for (const taskType of availableBackendTasks.value) {
    result.add(taskType.category || '');
  }
  return result;
});

// Computed values
const drawerRight = computed(() => selectedImageIds.value.length > 0);
const lastSelectedImage = computed(() =>
  projectImages.value.getImageById(
    selectedImageIds.value.length > 0
      ? selectedImageIds.value[selectedImageIds.value.length - 1]
      : -1
  )
);
const selectedImages = computed(() =>
  selectedImageIds.value.map((id) => projectImages.value.getImageById(id))
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
  onDialogYes(
    'Delete project',
    'Do your really want to delete the current project?'
  ).then(() => {
    api.post(`/project/${projectId}/delete`, {}).then(() => {
      router.push('/');
    });
  });
}

function deleteSelectedImages() {
  if (selectedImageIds.value.length > 0) {
    onDialogYes(
      'Delete images',
      'Do your really want to delete the selected images?'
    ).then(() => {
      const promises = [];
      for (const id of selectedImageIds.value) {
        promises.push(api.post(`/image/${id}/delete`));
      }
      Promise.all(promises).then(() => {
        queryBackend();
      });
    });
  }
}

function downloadSelectedImages() {
  for (const id of selectedImageIds.value) {
    downloadFromApi(
      `/image/${id}/raw`,
      ensureExtension(projectImages.value.getImageById(id).fileName, ['.png'])
    );
  }
}

function selectAll() {
  if (drawerUnsortedImages.value) {
    selectedImageIds.value = [...projectImages.value.imageIds];
  } else {
    selectedImageIds.value = [
      ...projectImages.value.imageIds.filter(
        (id) => projectImages.value.getImageById(id).groupColumn >= 0
      ),
    ];
  }
}

function doFrontEndProcessor(tool: FrontEndImageProcessor) {
  if (selectedImageIds.value && projectImages.value) {
    tool
      .fn(selectedImages.value, projectImages.value)
      .then((response) => {
        sendSuccessNotification(`Successfully applied "${tool.label}"`);
        if (response.needsUpload) {
          projectImages.value
            .uploadToBackend()
            .then(queryBackend)
            .catch(() =>
              sendFailureNotification(`Failed to update selected images`)
            );
        } else if (response.needsFullReload) {
          queryBackend();
        }
      })
      .catch(() => {
        sendFailureNotification(`Error while applying "${tool.label}"`);
      })
      .finally(() => {});
  }
}

function doBackendTaskClicked(tool: BackendTaskTypePayload) {
  if (selectedImageIds.value && projectImages.value) {
    doBackendTask(
      [...selectedImages.value],
      Number(projectId),
      tool,
      projectImages.value
    );
  }
}

function onTaskFinished() {
  // For now just query the backend again
  queryBackend();
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

      // Un-select the images
      // selectedImageIds.value = [];

      // Set up the unsorted images drawer
      drawerUnsortedImages.value =
        projectImages.value.unsortedRow.images.length > 0;
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

