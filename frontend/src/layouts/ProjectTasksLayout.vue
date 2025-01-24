<template>
  <q-layout view="hHh lpR fFf">
    <q-header>
      <q-toolbar>
        <q-toolbar-title class="row items-center q-gutter-sm">
          <HeaderLogoButtonComponent />
          <div>/</div>
          <q-skeleton
            v-if="!projectPayload.name"
            type="text"
            style="width: 200px"
          />
          <router-link
            style="text-decoration: underline; color: inherit"
            v-else
            :to="`/project/${projectId}`"
            >{{ projectPayload.name }}
          </router-link>
          <div>/</div>
          <div>Tasks</div>
        </q-toolbar-title>
        <AuthManagerComponent />
      </q-toolbar>
      <q-toolbar class="bg-primary text-white edit-toolbar">
        <q-btn color="secondary" icon="clear_all" @click="clearAll"
          >Clear
        </q-btn>
        <q-btn color="green" icon="download" label="Download log" v-if="!currentlyDisplayedTask.isRunning() && currentlyDisplayedTask.id > 0" @click="downloadFullLog"/>
        <q-btn color="red-4" icon="cancel" label="Cancel task" v-if="currentlyDisplayedTask.isRunning() && currentlyDisplayedTask.id > 0" @click="cancelTask"/>
        <ToggleButton v-model="autoScrollEnabled" color="blue-5" selected-icon="fa-solid fa-square-check" not-selected-icon="fa-solid fa-square" label="Auto scroll" v-if="currentlyDisplayedTask.isRunning() && currentlyDisplayedTask.id > 0" @click="downloadFullLog"/>
        <div class="col-grow" />
        <ProjectResultsButton :project-id="projectId[0]" v-model="resultList"/>
        <ProjectBackendTaskButton
          :project-id="projectId[0]"
          v-model="projectBackendTasks"
        />
      </q-toolbar>
    </q-header>
    <q-drawer
      side="left"
      :model-value="true"
      elevated
      class="q-pa-sm q-gutter-sm"
    >
      <q-card class="item" v-if="projectBackendTasks.length === 0" bordered>
        <q-card-section> There are currently no tasks.</q-card-section>
      </q-card>
      <q-list>
        <q-item clickable v-ripple v-for="task in sortedTasks"
                :key="task.id" @click="switchToTask(task)" :active="task.id == currentlyDisplayedTask.id">
          <q-item-section avatar>
            <q-icon name="check" v-if="task.status == TaskStatus.Successful" />
            <q-icon name="cancel" v-if="task.status == TaskStatus.Failed" />
            <q-spinner
              v-if="
              task.status == TaskStatus.Running ||
              task.status == TaskStatus.Ready
            "
            />
          </q-item-section>
          <q-item-section>
            <div>{{ task.name || 'Unnamed' }}</div>
            <div class="text-caption">
              {{ task.createdAt || 'Unknown creation date' }}
            </div>
          </q-item-section>
        </q-item>
      </q-list>
    </q-drawer>
    <q-page-container>
      <q-page padding class="flex column q-gutter-sm log">
        <q-card v-if="currentlyDisplayedTask.id <= 0" class="bg-blue-grey-4 text-white">
          <q-card-section>
            No task selected.
          </q-card-section>
        </q-card>
        <q-scroll-area ref="log-scroll-area" class="log-content" v-if="currentlyDisplayedTask.id > 0">
          <pre>{{ logText || "Loading..." }}</pre>
        </q-scroll-area>
        <q-linear-progress :indeterminate="progressInfoIsIndeterminate(progress)" :value="progressInfoValue(progress)" class="log-progress" v-if="currentlyDisplayedTask.id > 0 && currentlyDisplayedTask.isRunning()"/>
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router';
import HeaderLogoButtonComponent from 'components/layout/HeaderLogoButtonComponent.vue';
import {computed, onMounted, ref, Ref, useTemplateRef} from 'vue';
import {downloadFromApi, loadPayloadInstanceFromApi} from 'src/types/common';
import { ProjectMetadataPayload } from 'src/types/project';
import {
  BackendTaskPayload,
  extractProgressInfoFromLog,
  ProgressInfo,
  progressInfoIsIndeterminate, progressInfoValue,
  TaskStatus
} from 'src/types/backendTasks';
import { useIntervalFn } from '@vueuse/core';
import { api } from 'boot/axios';
import {QScrollArea, useQuasar} from 'quasar';
import ProjectBackendTaskButton from 'components/layout/ProjectBackendTaskButton.vue';
import AuthManagerComponent from 'components/layout/AuthManagerComponent.vue';
import ProjectResultsButton from 'components/layout/ProjectResultsButton.vue';
import { ResultPayload } from 'src/types/results';
import ToggleButton from "components/utils/ToggleButton.vue";
import {onDialogYes} from "src/types/dialog";


const logScrollAreaComponent = useTemplateRef<QScrollArea>("log-scroll-area")
const autoScrollEnabled = ref(true);

const progress = ref<ProgressInfo | null>(null);
let lastMatchTime = 0;


const $route = useRoute();
const $q = useQuasar();
const projectId = $route.params.id;
const projectPayload: Ref<ProjectMetadataPayload> = ref(
  new ProjectMetadataPayload()
);
const projectBackendTasks = ref<Array<BackendTaskPayload>>([]);
const resultList = ref<ResultPayload[]>();

const sortedTasks = computed(() => {
  return [...projectBackendTasks.value].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
})
const currentlyDisplayedTaskId = ref(-1)
const currentlyDisplayedTask = computed(() => {
  for(const task of projectBackendTasks.value) {
    if(task.id == currentlyDisplayedTaskId.value) {
      return task;
    }
  }
  return new BackendTaskPayload()
})
const logText = ref("")
const logNeedsUpdating = ref(false);

function clearAll() {
  $q.loading.show();
  api
    .post<BackendTaskPayload[]>(`/project/${projectId}/clear-tasks`)
    .then(() => {
      queryTaskBackend();
    })
    .finally(() => {
      $q.loading.hide();
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
    ResultPayload,
    resultList
  );
}

function switchToTask(task : BackendTaskPayload) {
  logText.value = ""
  currentlyDisplayedTaskId.value = task.id;
  logNeedsUpdating.value = true;
}

function parseRunningProgress(text: string) {
  const newProgress = extractProgressInfoFromLog(text)

  // Update progress bar
  if(newProgress) {
    progress.value = newProgress;
    lastMatchTime = Date.now(); // Update the last match timestamp
  }
  if (!newProgress && Date.now() - lastMatchTime > 10000) {
    // Reset progress to null if 10 seconds have elapsed without a match
    progress.value = null;
  }
}

function updateLog() {
  if(currentlyDisplayedTask.value.id > 0 && logNeedsUpdating.value) {
    if(currentlyDisplayedTask.value.isRunning()) {
      api.get(`/task/${currentlyDisplayedTask.value.id}/running-log`).then((data) => {
        parseRunningProgress(data.data + "")
        logText.value += (data.data + "")
        if(autoScrollEnabled.value) {
          logScrollAreaComponent.value?.setScrollPercentage("vertical", 1.0, 2200)
        }
      })
    }
    else {
      // Download full log
      api.get(`/task/${currentlyDisplayedTask.value.id}/log`).then((data) => {
        logText.value = (data.data + "")
      })
        .catch(() => {
          logText.value = "Error while loading log";
        })
        .finally(() => {
          logNeedsUpdating.value = false;
        })
    }
  }
}

function downloadFullLog() {
  if(currentlyDisplayedTask.value.id > 0 && currentlyDisplayedTask.value.isRunning()) {
    downloadFromApi(`/task/${currentlyDisplayedTask.value.id}/log`, "log.txt")
  }
}

function cancelTask() {
  onDialogYes("Cancel task '" + currentlyDisplayedTask.value.name + "'",
  "Do you really want to cancel the selected task?").then(() => {
    api.post(`/task/${currentlyDisplayedTask.value.id}/cancel`, {})
  })
}

onMounted(() => {
  loadPayloadInstanceFromApi(
    `/project/${projectId}`,
    ProjectMetadataPayload,
    projectPayload
  );
  queryTaskBackend();
  queryResultListBackend();
});

useIntervalFn(queryTaskBackend, 2500);
useIntervalFn(queryResultListBackend, 4000);
useIntervalFn(updateLog, 2500);
</script>
<style scoped lang="scss">
.log-content {
  font-family: monospace;
  flex-grow: 1;
  height: 200px;
}
</style>

