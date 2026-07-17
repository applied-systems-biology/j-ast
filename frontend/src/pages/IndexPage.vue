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
  <q-page class="page-container">
    <q-toolbar v-if="authStore.isLoggedIn" class="bg-primary edit-toolbar">
      <q-btn
        :disable="!authStore.isLoggedIn || !canAddProject"
        icon="add"
        color="green"
        @click="newProject"
        label="New project"
        no-caps
        no-wrap />
      <q-btn
        :disable="!authStore.isLoggedIn"
        icon="refresh"
        color="blue-5"
        @click="refreshProjectList"
        label="Refresh"
        no-caps
        no-wrap />
    </q-toolbar>
    <q-scroll-area class="q-pa-md project-list" v-if="authStore.isLoggedIn">
      <q-toolbar class="bg-white text-black edit-toolbar">
        <q-input v-model="filterText" class="q-ma-sm" clearable debounce="1000" dense outlined
                 style="width: 500px; max-width: 50vw;">
          <template v-slot:prepend>
            <q-icon name="search"/>
          </template>
        </q-input>
      </q-toolbar>
      <q-table flat :rows="projectTableRows" :columns="projectTableColumns" :pagination="filesViewPagination"
               @row-click="onRowClick" row-key="key">
        <template v-slot:body-cell-thumbnail="props">
          <q-td :props="props">
            <q-icon class="thumbnail" name="fa-solid fa-folder" color="blue" size="xl"/>
          </q-td>
        </template>
        <template v-slot:body-cell-owner="props">
          <q-td :props="props">
            <div class="row items-center">
              <q-icon class="q-mr-sm" name="fa-solid fa-user" />
              <div>{{ props.row.owner || 'Admin' }}</div>
            </div>
          </q-td>
        </template>
        <template v-slot:body-cell-actions="props">
          <q-td :props="props">
            <div class="q-gutter-sm" v-if="props.row.id >= 0">
              <q-btn icon="search" @click.stop="openProject(props.row.id)"/>
              <q-btn icon="delete" @click.stop="deleteProject(props.row.id)"/>
            </div>
          </q-td>
        </template>
      </q-table>
    </q-scroll-area>
    <q-scroll-area class="project-list" v-if="!authStore.isLoggedIn">
      <div class="bg-blue-grey-2 q-pt-lg q-pb-lg hero">
        <img class="q-mt-lg q-mb-lg logo" :src="logo" alt="J-AST logo" />
        <div class="text-h5 text-weight-light">
          Your tool for managing, annotating, and analyzing disk diffusion
          assays and E-tests
        </div>
      </div>
      <div class="q-pa-md q-gutter-sm features-list">
        <q-card class="feature-list-item">
          <q-card-section horizontal>
            <q-img class="col-5" :src="heroUpload" />
            <q-card-section class="col">
              <div class="text-overline">Feature</div>
              <div class="text-h5 q-mt-sm q-mb-xs">Upload your data</div>
              <div>
                Use the uploader component to store your data into a J-AST
                project.
                <p>You can later at any time download your data as *.zip.</p>
              </div>
            </q-card-section>
          </q-card-section>
        </q-card>
        <q-card class="feature-list-item">
          <q-card-section horizontal>
            <q-img class="col-5" :src="heroArrangeTimeSeriues" />
            <q-card-section class="col">
              <div class="text-overline">Feature</div>
              <div class="text-h5 q-mt-sm q-mb-xs">Annotate and arrange</div>
              <div>
                Annotate your data with essential metadata by either doing the
                work manually or using our automated tool that extracts the
                information from the file name.
                <p>
                  Then you can proceed to either arrange your time lines
                  manually via drag and drop or use the included automated tool
                  that utilizes the metadata.
                </p>
              </div>
            </q-card-section>
          </q-card-section>
        </q-card>
        <q-card class="feature-list-item">
          <q-card-section horizontal>
            <q-img class="col-5" :src="heroProcess" />
            <q-card-section class="col">
              <div class="text-overline">Feature</div>
              <div class="text-h5 q-mt-sm q-mb-xs">Automated processing</div>
              <div>
                Select which data to process and run a variety of automated
                image processing and analysis algorithms directly from within
                J-AST.
              </div>
            </q-card-section>
          </q-card-section>
        </q-card>
        <q-card class="feature-list-item">
          <q-card-section horizontal>
            <q-img class="col-5" :src="heroInteractiveAnnotation" />
            <q-card-section class="col">
              <div class="text-overline">Feature</div>
              <div class="text-h5 q-mt-sm q-mb-xs">
                Manually guide the automated analysis
              </div>
              <div>
                The automated detection algorithms don't work or yield
                unsatisfactory results? Don't worry - all annotations can be
                edited manually directly within J-AST.
              </div>
            </q-card-section>
          </q-card-section>
        </q-card>
        <q-card class="feature-list-item">
          <q-card-section horizontal>
            <q-img class="col-5" :src="heroResultsBrowser" />
            <q-card-section class="col">
              <div class="text-overline">Feature</div>
              <div class="text-h5 q-mt-sm q-mb-xs">Review results</div>
              <div>
                Annotate your data with essential metadata by either doing the
                work manually or using our automated tool that extracts the
                information from the file name.
                <p>
                  Then you can proceed to either arrange your time lines
                  manually via drag and drop or use the included automated tool
                  that utilizes the metadata.
                </p>
              </div>
            </q-card-section>
          </q-card-section>
        </q-card>
      </div>
    </q-scroll-area>
  </q-page>
</template>
<script setup lang="ts">
import {Dialog, QSpinnerHourglass, QTableColumn, useQuasar} from 'quasar';
import { useAuthStore } from 'stores/auth-store';
import { computed, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { api } from 'boot/axios';
import { useWatchInterval } from '../composables/UseWatchInterval';
import { useRouter } from 'vue-router';
import {
  sendFailureNotification,
  sendSuccessNotification,
} from 'src/types/notification';
import {
  CreateProjectRequest,
  ProjectMetadataPayload,
} from 'src/types/project';
import logo from 'assets/logo-j-ast-full.svg';
import heroUpload from 'assets/hero/hero-upload.png';
import heroArrangeTimeSeriues from 'assets/hero/hero-arrange-time-series.png';
import heroProcess from 'assets/hero/hero-process.png';
import heroInteractiveAnnotation from 'assets/hero/hero-interactive-annotation.png';
import heroResultsBrowser from 'assets/hero/hero-results-browser.png';
import CreateProjectDialog from 'components/CreateProjectDialog.vue';
import { plainToInstance } from 'class-transformer';
import { uploadProjectArchive } from 'src/types/projectArchive';
import {onDialogYes} from "src/types/dialog";

const $q = useQuasar();
const authStore = useAuthStore();
const router = useRouter();
const { isLoggedIn } = storeToRefs(authStore);
const projectList = ref<ProjectMetadataPayload[] | null>();
const canAddProject = computed(() => {
  if (authStore.isLoggedIn && projectList.value) {
    if (authStore.isGuest) {
      return projectList.value?.length < authStore.limits.guestMaxProjects;
    } else {
      return true;
    }
  } else {
    return false;
  }
});

const filterText = ref("")

const filesViewPagination = {
  rowsPerPage: 0
}

const projectTableColumns : QTableColumn[] = [
  {
    name: "thumbnail",
    label: "",
    field: "id",
    sortable: false,
    align: 'center',
    style: 'width: 120px; height: 120px;',
  },
  {
    name: "name",
    label: "Name",
    field: "name",
    sortable: true,
    align: 'left',
  },
  {
    name: "owner",
    label: "Owner",
    field: "owner",
    sortable: true,
    align: 'left',
  },
  {
    name: "updatedAt",
    label: "Last Modified",
    field: "updatedAt",
    sortable: false,
    align: 'left',
    format: (val: string) => {
      if (!val) return '-'
      const date = new Date(val)
      return date.toLocaleString()
    }
  },
  {
    name: "id",
    label: "ID",
    field: "id",
    sortable: true,
    align: 'left',
  },
  {
    name: "actions",
    label: "",
    field: "id",
    sortable: false,
    align: 'right',
  }
]

const projectTableRows = computed(() => {
  if (!projectList.value) {
    return []
  }

  // Create a filtered copy if filter text exists
  const filteredProjects = filterText.value
      ? projectList.value.filter(project =>
          project.name?.toLowerCase().includes(filterText.value.toLowerCase())
      )
      : [...projectList.value]

  // Sort by updatedAt descending (most recent first)
  return filteredProjects.sort((a, b) => {
    const dateA = a.updatedAt ? new Date(a.updatedAt).getTime() : 0
    const dateB = b.updatedAt ? new Date(b.updatedAt).getTime() : 0
    return dateB - dateA  // Descending order (newest first)
  })
})

function onRowClick(evt: any, row: ProjectMetadataPayload) {
  openProject(row.id)
}


/**
 * Creates a new project
 */
function newProject() {
  Dialog.create({
    component: CreateProjectDialog,
    componentProps: {
      persistent: true,
    },
  }).onOk((payload: CreateProjectRequest) => {
    api
      .post('/new-project', payload)
      .then((result) => {
        const info = plainToInstance(ProjectMetadataPayload, result.data);
        sendSuccessNotification(`Created new project "${info.name}"`);

        if (payload.projectArchiveFile) {
          // Upload the project archive
          doUploadProjectArchive(info.id, payload.projectArchiveFile);
        } else {
          refreshProjectList();
        }
      })
      .catch((reason) => {
        console.log(reason);
        sendFailureNotification('Unable to create project!');
      });
  });
}

/**
 * Uploads a project archive file to the backend for the import process
 * @param id the project id
 * @param projectArchiveFile the archive file
 */
function doUploadProjectArchive(id: number, projectArchiveFile: File) {
  const shouldCancel = ref<boolean>(false);
  const dialog = $q.dialog({
    title: 'Uploading project ...',
    message: 'Preparing ...',
    progress: {
      spinner: QSpinnerHourglass,
    },
    persistent: true,
    ok: false,
    cancel: true,
  });
  dialog.onCancel(() => {
    shouldCancel.value = true;
    dialog.hide();
  });

  uploadProjectArchive(id, projectArchiveFile, (percentage, info) => {
      dialog.update({
        message: `${percentage}% ${info}`,
      });
    },
    () => shouldCancel.value).finally(() => {
    dialog.hide();
    refreshProjectList()
  });
}

/**
 * Populate/clear the project list
 */
function refreshProjectList() {
  projectList.value = null;
  if (isLoggedIn.value) {
    api.get<ProjectMetadataPayload[]>('/list-projects').then((result) => {
      projectList.value = result.data;
    });
  } else {
    projectList.value = null;
  }
}

function openProject(id: number) {
  router.push(`/project/${id}`);
}

function deleteProject(id: number) {
  onDialogYes(
      'Delete project',
      'Do your really want to delete the project?'
  ).then(() => {
    $q.loading.show({
      message: 'This may take some time for large projects ...'
    })
    api.post(`/project/${id}/delete`, {}).then(() => {
      refreshProjectList()
    })
        .finally(() => {
          $q.loading.hide()
        })
  });
}

/**
 * Watch for auto-updating the project list
 */
useWatchInterval(isLoggedIn, () => {
  refreshProjectList();
});
</script>
<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
}

.project-item {
  width: 12rem;
  height: 12rem;
}

.project-list {
  flex-grow: 1;
  height: 200px;
}

.hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-grow: 1;

  .logo {
    width: 50vw;
    max-width: 1024px;
  }
}

.features-list {
  display: flex;
  flex-direction: column;
  align-items: center;

  .feature-list-item {
    width: 50vw;
  }
}
</style>
