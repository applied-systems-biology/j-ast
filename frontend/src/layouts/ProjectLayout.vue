<template>
  <q-layout view="hHh lpR fFf">
    <q-header>
      <q-toolbar>
        <q-toolbar-title class="row items-center q-gutter-sm">
          <HeaderLogoButtonComponent />
          <div>/</div>
          <q-skeleton v-if="!projectName" type="text" style="width: 200px" />
          <div v-else>{{ projectName }}</div>
          <q-btn-group>
            <q-btn color="blue" @click="editProjectName" :disable="hasTaskRunning">
              <q-icon name="edit" />
            </q-btn>
            <q-btn color="red-4" @click="deleteProject" :disable="hasTaskRunning">
              <q-icon name="delete" />
            </q-btn>
          </q-btn-group>
          <q-btn-dropdown color="blue" :label="currentViewMode" :icon="currentViewMode == ViewMode.Timeline ? 'fa-solid fa-timeline' : 'fa-solid fa-grip'">
            <q-list>
              <q-item clickable v-close-popup @click="changeViewMode(ViewMode.Timeline)">
                <q-item-section avatar>
                  <q-icon name="fa-solid fa-timeline" />
                </q-item-section>
                <q-item-section>
                  <q-item-label>Timeline</q-item-label>
                </q-item-section>
                <q-tooltip>
                  Use this view for projects that have a time component. You will need to sort the images into timelines.
                </q-tooltip>
              </q-item>
              <q-item clickable v-close-popup @click="changeViewMode(ViewMode.Grid)">
                <q-item-section avatar>
                  <q-icon name="fa-solid fa-grip" />
                </q-item-section>
                <q-item-section>
                  <q-item-label>Single images</q-item-label>
                </q-item-section>
                <q-tooltip>
                  Use this view for projects where each image is independent.
                </q-tooltip>
              </q-item>
            </q-list>
          </q-btn-dropdown>
          <q-btn color="blue" @click="goToBrowser" label="Browse" no-caps icon="fa-solid fa-magnifying-glass"/>
        </q-toolbar-title>
        <AuthManagerComponent />
        <DocumentationComponent/>
      </q-toolbar>
      <q-toolbar class="bg-primary text-white edit-toolbar">
        <ToggleButton
          not-selected-icon="upload"
          selected-icon="close"
          class="bg-green"
          v-model="drawerLeft"
        >
          Upload
          <q-tooltip>
            Allows you to upload raw image files. Please note that all new
            images will be put into the "Unsorted images" list.
          </q-tooltip>
        </ToggleButton>
        <q-btn-dropdown color="green" icon="download" label="Download">
          <q-list>
            <q-item v-if="selectedImageIds.length > 0" clickable v-close-popup @click="downloadZip(selectedImageIds)">
              <q-item-section>
                <q-item-label>Download selected images and annotations (*.zip)</q-item-label>
              </q-item-section>
            </q-item>
            <q-item v-if="selectedImageIds.length > 0" clickable v-close-popup @click="downloadSelectedImages">
              <q-item-section>
                <q-item-label>Download selected raw images (*.png)</q-item-label>
              </q-item-section>
            </q-item>
            <q-item v-if="selectedImageIds.length > 0" clickable v-close-popup @click="downloadProjectArchive(selectedImageIds)">
              <q-item-section>
                <q-item-label>Export selection as project archive (*.project.zip)</q-item-label>
                <q-tooltip>Creates an archive based on the selected images that can be later imported into another J-AST instance</q-tooltip>
              </q-item-section>
            </q-item>
            <q-separator v-if="selectedImageIds.length > 0"/>
            <q-item clickable v-close-popup @click="downloadZip(null)">
              <q-item-section>
                <q-item-label>Download all images (*.zip)</q-item-label>
              </q-item-section>
            </q-item>
            <q-item clickable v-close-popup @click="downloadProjectArchive(null)">
              <q-item-section>
                <q-item-label>Export project (*.project.zip)</q-item-label>
                <q-tooltip>Creates an archive that can be later imported into another J-AST instance</q-tooltip>
              </q-item-section>
            </q-item>
          </q-list>
        </q-btn-dropdown>
        <ToggleButton
          v-if="currentViewMode == ViewMode.Timeline"
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
        <ToggleButton
          selected-icon="fa-solid fa-filter-circle-xmark"
          not-selected-icon="fa-solid fa-filter"
          class="bg-secondary"
          v-model="toolbarFilter"
        >
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
          color="accent"
          label="Process"
          icon="fa-solid fa-gear"
          v-if="selectedImageIds.length > 0"
          :disable="hasTaskRunning"
        >
          <q-menu>
            <q-list style="min-width: 100px">
              <!-- Front-end processors -->
              <template  v-for="tool in frontEndImageProcessors"
                         :key="tool.label">
                <q-item
                  v-if="tool.viewMode == undefined || tool.viewMode == currentViewMode"
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
              </template>
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
                    :disable="tool.viewModeRestriction != currentViewMode && tool.viewModeRestriction != null"
                    clickable
                    v-close-popup
                    @click="doBackendTaskClicked(tool)"
                  >
                    <q-item-section avatar>
                      <q-icon name="fa-solid fa-wand-magic-sparkles" />
                    </q-item-section>
                    <q-item-section>{{ tool.name }}</q-item-section>
                    <q-tooltip>{{ tool.shortDescription }}</q-tooltip>
                  </q-item>
                </q-menu>
              </q-item>
              <q-item
                v-for="tool in availableBackendTasks.filter(
                  (task) => !task.category
                )"
                :disable="tool.viewModeRestriction != currentViewMode && tool.viewModeRestriction != null"
                :key="tool.taskId"
                clickable
                v-close-popup
                @click="doBackendTaskClicked(tool)"
              >
                <q-item-section avatar>
                  <q-icon name="fa-solid fa-wand-magic-sparkles" />
                </q-item-section>
                <q-item-section>{{ tool.name }}</q-item-section>
                <q-tooltip>{{ tool.shortDescription }}</q-tooltip>
              </q-item>
            </q-list>
          </q-menu>
        </q-btn>
        <q-btn
          icon="delete"
          color="red-4"
          v-if="selectedImageIds.length > 0"
          @click="deleteSelectedImages"
          :disable="hasTaskRunning"
        >
          <q-tooltip> Deletes the selected image(s)</q-tooltip>
        </q-btn>
        <q-btn
          icon="refresh"
          color="primary"
          @click="queryBackend"
          :disable="hasTaskRunning"
        >
          <q-tooltip>Reloads the view</q-tooltip>
        </q-btn>
        <div class="col-grow" />
        <ProjectResultsButton
          :project-id="projectId"
          v-model="resultList"/>
        <ProjectBackendTaskButton
          :project-id="projectId"
          v-model="projectBackendTasks"
          @on-task-finished="onTaskFinished"
        />
      </q-toolbar>
      <q-toolbar class="bg-white text-black edit-toolbar" v-if="toolbarFilter">
        <q-input style="width: 500px; max-width: 50vw;" v-model="filterText" dense outlined clearable class="q-ma-sm" debounce="1000">
          <template v-slot:prepend>
            <q-icon name="search" />
          </template>
        </q-input>
        <q-btn label="No plate" no-caps flat icon="filter_alt" @click="filterText = 'hasPlate:no'" dense />
        <q-btn label="No disk/strip" no-caps flat icon="filter_alt" @click="filterText = 'hasDiskStrip:no'" dense />
        <q-btn label="No ZOI shape" no-caps flat icon="filter_alt" @click="filterText = 'hasZOIShape:no'" dense />
        <q-btn label="More ..." no-caps flat icon="filter_alt" dense>
          <q-menu>
            <q-item clickable @click="filterText = 'experiment:'" v-close-popup>
              <q-item-section avatar>
                <q-icon name="fa-solid fa-vial-virus" />
              </q-item-section>
              <q-item-section>Search for experiment</q-item-section>
            </q-item>
            <q-item clickable @click="filterText = 'sample:'" v-close-popup>
              <q-item-section avatar>
                <q-icon name="fa-solid fa-flask" />
              </q-item-section>
              <q-item-section>Search for sample</q-item-section>
            </q-item>
            <q-item clickable @click="filterText = 'assayType:DDA'" v-close-popup>
              <q-item-section avatar>
                <q-icon name="fa-solid fa-filter" />
              </q-item-section>
              <q-item-section>Only DDA</q-item-section>
            </q-item>
            <q-item clickable @click="filterText = 'assayType:ETest'" v-close-popup>
              <q-item-section avatar>
                <q-icon name="fa-solid fa-filter" />
              </q-item-section>
              <q-item-section>Only E-Test</q-item-section>
            </q-item>
          </q-menu>
        </q-btn>
      </q-toolbar>
    </q-header>
    <q-drawer elevated side="left" bordered v-model="drawerLeft" overlay>
      <ImageUploaderComponent
        :project-id="projectId"
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
        <TimelineViewComponent
          ref="timelineViewComponent"
          v-if="currentViewMode == ViewMode.Timeline"
          v-model="projectImages"
          v-model:selected-image-ids="selectedImageIds"
          v-model:backend-tasks="projectBackendTasks"
          v-model:filter-text="filterText"
          :show-unsorted="drawerUnsortedImages"
        />
        <GridViewComponent
          ref="gridViewComponent"
          v-if="currentViewMode == ViewMode.Grid"
          v-model="projectImages"
          v-model:selected-image-ids="selectedImageIds"
          v-model:backend-tasks="projectBackendTasks"
          v-model:filter-text="filterText"
        />
        <BackendTaskProgressOverlay  v-model:backend-tasks="projectBackendTasks" />
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import AuthManagerComponent from 'components/layout/AuthManagerComponent.vue';
import HeaderLogoButtonComponent from 'components/layout/HeaderLogoButtonComponent.vue';
import {computed, onMounted, ref, Ref, useTemplateRef} from 'vue';
import ToggleButton from 'components/utils/ToggleButton.vue';
import ImageUploaderComponent from 'components/drawers/ImageUploaderComponent.vue';
import { QSpinnerHourglass, useQuasar } from "quasar";
// import { VueDraggableNext as draggable } from 'vue-draggable-next';
import { useRoute, useRouter } from 'vue-router';
import { api } from 'boot/axios';
import { downloadFromApi, ensureExtension, loadPayloadInstanceFromApi, removeExtensionIfPresent } from "src/types/common";
import ProjectImageEditor from 'components/drawers/ProjectImageEditor.vue';
import { plainToInstance } from 'class-transformer';
import TimelineViewComponent from 'components/arranger/TimelineViewComponent.vue';
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
import { ResultPayload } from "src/types/results";
import { generateAndDownloadZip, ZipItem } from "src/types/zip";
import { formatFileSize } from "src/types/utils";
import BackendTaskProgressOverlay from 'components/backendProcessors/BackendTaskProgressOverlay.vue';
import DocumentationComponent from "components/layout/DocumentationComponent.vue";
import { ViewMode } from 'src/types/view';
import GridViewComponent from 'components/arranger/GridViewComponent.vue';
import { ImagePayload } from 'src/types/image';

function createDummyBackendTask() {
  const task = new BackendTaskPayload()
  task.name = "Waiting for server ..."
  return task
}

const $q = useQuasar();
const $route = useRoute();
const router = useRouter();
const drawerLeft: Ref<boolean> = ref(false);
const drawerUnsortedImages = ref(false);
const toolbarFilter = ref(false)
const projectName = computed(() => projectPayload.value?.name ?? undefined);
const projectId = $route.params.id + ""
const projectPayload: Ref<ProjectMetadataPayload> = ref(
  new ProjectMetadataPayload()
);
const projectImages = ref<ProjectImagesPayload>(new ProjectImagesPayload());
const selectedImageIds = ref<Array<number>>([]);
const availableBackendTasks = ref<Array<BackendTaskTypePayload>>([]);
const projectBackendTasks = ref<Array<BackendTaskPayload>>([ createDummyBackendTask() ]);
const resultList = ref<ResultPayload[]>();
const filterText = ref("")
const currentViewMode = ref(ViewMode.Timeline);

const timelineViewComponent = useTemplateRef<any>("timelineViewComponent");
const gridViewComponent = useTemplateRef<any>("gridViewComponent");

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

const availableBackendTasksCategories = computed(() => {
  const predefinedOrder = ["Preprocessing", "Plate", "DDA", "E-Test", "Analyze"];
  const result = new Set<string>();
  for (const taskType of availableBackendTasks.value) {
    result.add(taskType.category || '');
  }

  // Convert the Set to an Array and sort
  const predefinedOrderSet = new Set(predefinedOrder);
  const sortedList = Array.from(result).sort((a, b) => {
    const indexA = predefinedOrderSet.has(a) ? predefinedOrder.indexOf(a) : predefinedOrder.length;
    const indexB = predefinedOrderSet.has(b) ? predefinedOrder.indexOf(b) : predefinedOrder.length;
    if (indexA !== indexB) {
      return indexA - indexB; // Sort by predefined order
    }
    return a.localeCompare(b); // Sort alphabetically for items not in the predefined order
  });

  return sortedList.filter((item) => !!item);
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
        viewMode: currentViewMode.value
      } as CreateEditProjectRequest)
      .then((response) => {
        projectPayload.value = plainToInstance(
          ProjectMetadataPayload,
          response.data
        );
      });
  });
}

function changeViewMode(newViewMode: ViewMode){
  currentViewMode.value = newViewMode
  api
    .post<CreateEditProjectRequest>(`/project/${projectId}/edit`, {
      name: projectPayload.value.name,
      viewMode: newViewMode
    } as CreateEditProjectRequest)
    .then((response) => {
      projectPayload.value = plainToInstance(
        ProjectMetadataPayload,
        response.data
      );
  });
}

function goToBrowser() {
  router.push(`/browse/${projectId}`);
}

function deleteProject() {
  onDialogYes(
    'Delete project',
    'Do your really want to delete the current project?'
  ).then(() => {
    $q.loading.show({
      message: 'This may take some time for large projects ...'
    })
    api.post(`/project/${projectId}/delete`, {}).then(() => {
      router.push('/');
    })
    .finally(() => {
      $q.loading.hide()
    })
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

function downloadZip(imageIds : Array<number> | null) {
  if(imageIds == null) {
    imageIds = projectImages.value.imageIds
  }

  // Map to images
  const items = imageIds.map(id => projectImages.value.getImageById(id));

  // Create Zip items
  const zipItems : Array<ZipItem>  = []
  const usedFileNames = new Set<string>()
  let downloadSizeBytes = 0
  for(const item of items) {
    let fileName = item.fileName || `${item.assayType}_${item.experiment}_${item.sample}_${item.timePoint}`
    fileName = removeExtensionIfPresent(fileName)
    if(usedFileNames.has(fileName)) {
      fileName = fileName + "_" + item.id
    }
    usedFileNames.add(fileName)
    zipItems.push({ entryName: ensureExtension(fileName), url: `/image/${item.id}/raw`, content: null })
    downloadSizeBytes += item.size

    // Add annotations
    for(const annotation of item.maskImageAnnotations) {
      zipItems.push({ entryName: ensureExtension(fileName + "_" + annotation.annotationTypeId), url: `/mask-image-annotation/${item.id}/${annotation.annotationTypeId}/raw`, content: null })
      downloadSizeBytes += annotation.size
    }
  }

  $q.dialog({
    title: 'Download inputs',
    message: `You are about to download ${zipItems.length} files (${formatFileSize(downloadSizeBytes)}).<br/>Do you want to continue?<br/><br/>Please note that due how the ZIP file is created, your computer needs at least ${formatFileSize(downloadSizeBytes)} of free RAM space.`,
    html: true,
    cancel: true,
    persistent: true
  }).onOk(() => {
    const shouldCancel = ref<boolean>(false);
    const dialog = $q.dialog({
      title: 'Downloading files ...',
      message: 'Preparing ...',
      progress: {
        spinner: QSpinnerHourglass,
      },
      persistent: true,
      ok: false,
      cancel: true,
    })
    dialog.onCancel(() => {
      shouldCancel.value = true
      dialog.hide()
    })

    generateAndDownloadZip(zipItems, projectPayload.value.name, (percentage, info) => {
      dialog.update({
        message: `${percentage}% ${info}`
      })
    }, () => shouldCancel.value)
      .finally(() => {
        dialog.hide()
      })

  })
}

function downloadProjectArchive(imageIds : Array<number> | null) {

  if(imageIds == null) {
    imageIds = projectImages.value.imageIds
  }

  // Map to images
  const items = imageIds.map(id => projectImages.value.getImageById(id));

  // Create Zip items
  const zipItems : Array<ZipItem>  = []
  let downloadSizeBytes = 0
  const metadata : Record<string, any> = {}
  for(const item of items) {
    const fileName = "" + item.id
    zipItems.push({ entryName: ensureExtension(fileName), url: `/image/${item.id}/raw`, content: null })
    downloadSizeBytes += item.size

    // Add annotations
    for(const annotation of item.maskImageAnnotations) {
      const annotationFileName = annotation.annotationTypeId + "/" + fileName
      zipItems.push({ entryName: ensureExtension(annotationFileName), url: `/mask-image-annotation/${item.id}/${annotation.annotationTypeId}/raw`, content: null })
      downloadSizeBytes += annotation.size
    }

    // Add metadata
    metadata["" + item.id] = plainToInstance(ImagePayload, item).getMetadataAsDict()
  }

  // Create ZIP entry for metadata
  zipItems.push({ entryName: "metadata.json", url: null, content: JSON.stringify(metadata, null, 4) })

  $q.dialog({
    title: 'Download project archive',
    message: `You are about to download ${zipItems.length} files (${formatFileSize(downloadSizeBytes)}).<br/>Do you want to continue?<br/><br/>Please note that due how the ZIP file is created, your computer needs at least ${formatFileSize(downloadSizeBytes)} of free RAM space.`,
    html: true,
    cancel: true,
    persistent: true
  }).onOk(() => {
    const shouldCancel = ref<boolean>(false);
    const dialog = $q.dialog({
      title: 'Downloading project contents ...',
      message: 'Preparing ...',
      progress: {
        spinner: QSpinnerHourglass,
      },
      persistent: true,
      ok: false,
      cancel: true,
    })
    dialog.onCancel(() => {
      shouldCancel.value = true
      dialog.hide()
    })

    generateAndDownloadZip(zipItems, projectPayload.value.name + ".project.zip", (percentage, info) => {
      dialog.update({
        message: `${percentage}% ${info}`
      })
    }, () => shouldCancel.value)
      .finally(() => {
        dialog.hide()
      })

  })
}

function selectAll() {

  if(currentViewMode.value == ViewMode.Timeline) {
    if(timelineViewComponent.value) {
      timelineViewComponent.value.selectAll()
    }
  }
  else if(currentViewMode.value == ViewMode.Grid) {
    if(gridViewComponent.value) {
      gridViewComponent.value.selectAll()
    }
  }

  // if (drawerUnsortedImages.value) {
  //   selectedImageIds.value = [...projectImages.value.imageIds];
  // } else {
  //   selectedImageIds.value = [
  //     ...projectImages.value.imageIds.filter(
  //       (id) => projectImages.value.getImageById(id).groupColumn >= 0
  //     ),
  //   ];
  // }
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
    currentViewMode.value = projectPayload.value.viewMode
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

