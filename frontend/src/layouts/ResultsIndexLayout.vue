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
          <div>Results</div>
        </q-toolbar-title>
        <AuthManagerComponent />
      </q-toolbar>
      <q-toolbar class="bg-primary text-white edit-toolbar">
        <div class="col-grow" />
        <ProjectResultsButton :project-id="projectId[0]" v-model="resultList" />
        <ProjectBackendTaskButton
          :project-id="projectId[0]"
          v-model="projectBackendTasks"
        />
      </q-toolbar>
    </q-header>
    <q-page-container>
      <q-page padding class="q-gutter-sm">
        <q-card v-if="!resultList || resultList.length == 0">
          <q-card-section horizontal class="items-center">
            <q-card-section>
              <q-icon size="xl" name="fa-solid fa-file-circle-xmark" />
            </q-card-section>
            <q-card-section>
              <div class="text-h6">No results</div>
              <div class="text-subtitle2">This project has no results</div>
            </q-card-section>
          </q-card-section>
        </q-card>
        <q-card v-for="result in resultList" :key="result.id">
          <q-card-section horizontal class="items-center">
            <q-card-section class="col">
              <q-btn
                flat
                no-caps
                class="result-button"
                align="left"
                @click="goToResult(result.id)"
              >
                <div class="row q-gutter-lg">
                  <div>
                    <q-icon size="xl" name="fa-solid fa-folder" />
                  </div>
                  <div class="col text-left">
                    <div class="text-h6">{{ result.name }}</div>
                    <div class="text-caption">
                      <q-icon size="xs" name="fa-solid fa-circle-info fa-fw" />
                      {{ result.description }}
                    </div>
                    <div class="text-caption">
                      <q-icon size="xs" name="fa-solid fa-clock fa-fw" />
                      {{ result.createdAt }}
                    </div>
                  </div>
                </div>
              </q-btn>
            </q-card-section>
            <q-card-actions>
              <q-btn icon="delete" flat @click="deleteResult(result)" />
            </q-card-actions>
          </q-card-section>
        </q-card>
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import HeaderLogoButtonComponent from 'components/layout/HeaderLogoButtonComponent.vue';
import AuthManagerComponent from 'components/layout/AuthManagerComponent.vue';
import { useRoute, useRouter } from 'vue-router';
import { onMounted, ref, Ref } from 'vue';
import { ProjectMetadataPayload } from 'src/types/project';
import { loadPayloadInstanceFromApi } from 'src/types/common';
import ProjectBackendTaskButton from 'components/layout/ProjectBackendTaskButton.vue';
import { BackendTaskPayload } from 'src/types/backendTasks';
import { useIntervalFn } from '@vueuse/core';
import ProjectResultsButton from 'components/layout/ProjectResultsButton.vue';
import { ResultPayload } from 'src/types/results';
import { onDialogYes } from 'src/types/dialog';
import { api } from 'boot/axios';
import { sendFailureNotification, sendSuccessNotification } from 'src/types/notification';

const $route = useRoute();
const $router = useRouter();
const projectId = $route.params.id;
const projectPayload: Ref<ProjectMetadataPayload> = ref(
  new ProjectMetadataPayload()
);
const projectBackendTasks = ref<Array<BackendTaskPayload>>([]);
const resultList = ref<ResultPayload[]>();

defineOptions({
  name: 'ResultsIndexLayout',
});

function goToResult(id: number) {
  $router.push(`/results/view/${id}`);
}

function deleteResult(result: ResultPayload) {
  onDialogYes(
    'Delete result',
    `Do you really want to delete the result "${result.name}"?`
  ).then(() => {
    api
      .post(`/result/${result.id}/delete`)
      .then(() => {
        sendSuccessNotification("The result was deleted.");
        queryResultListBackend();
      })
      .catch(() => {
        sendFailureNotification('Unable to delete result');
      });
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
  queryResultListBackend();
  queryTaskBackend();
});
useIntervalFn(queryTaskBackend, 2500);
useIntervalFn(queryResultListBackend, 4000);
</script>
<style scoped lang="scss">
.result-button {
  width: 100%;
}
</style>
