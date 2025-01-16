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
        <q-item clickable v-ripple v-for="task in projectBackendTasks"
                :key="task.id">
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
      <q-page padding class="flex column q-gutter-sm">

      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router';
import HeaderLogoButtonComponent from 'components/layout/HeaderLogoButtonComponent.vue';
import { onMounted, ref, Ref } from 'vue';
import { loadPayloadInstanceFromApi } from 'src/types/common';
import { ProjectMetadataPayload } from 'src/types/project';
import { BackendTaskPayload, TaskStatus } from 'src/types/backendTasks';
import { useIntervalFn } from '@vueuse/core';
import { api } from 'boot/axios';
import { useQuasar } from 'quasar';
import ProjectBackendTaskButton from 'components/layout/ProjectBackendTaskButton.vue';
import AuthManagerComponent from 'components/layout/AuthManagerComponent.vue';
import ProjectResultsButton from 'components/layout/ProjectResultsButton.vue';
import { ResultPayload } from 'src/types/results';

const $route = useRoute();
const $q = useQuasar();
const projectId = $route.params.id;
const projectPayload: Ref<ProjectMetadataPayload> = ref(
  new ProjectMetadataPayload()
);
const projectBackendTasks = ref<Array<BackendTaskPayload>>([]);
const resultList = ref<ResultPayload[]>();

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
</script>

