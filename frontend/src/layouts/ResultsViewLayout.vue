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
        <ProjectResultsButton :project-id="projectId" v-model="resultList"/>
        <ProjectBackendTaskButton
          :project-id="projectId"
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
    </q-drawer>
  </q-layout>
</template>

<script setup lang="ts">
import HeaderLogoButtonComponent from 'components/layout/HeaderLogoButtonComponent.vue';
import AuthManagerComponent from 'components/layout/AuthManagerComponent.vue';
import { useRoute } from 'vue-router';
import { computed, onMounted, ref, Ref } from 'vue';
import { ProjectMetadataPayload } from 'src/types/project';
import { loadPayloadInstanceFromApi } from 'src/types/common';
import ProjectBackendTaskButton from 'components/layout/ProjectBackendTaskButton.vue';
import { BackendTaskPayload } from 'src/types/backendTasks';
import { useIntervalFn } from '@vueuse/core';
import ProjectResultsButton from 'components/layout/ProjectResultsButton.vue';
import { FullResultPayload, ResultPayload } from 'src/types/results';

const $route = useRoute();
const resultId = $route.params.id;
const projectId = computed(() => result.value.projectId ? result.value.projectId.toString() : '');
const projectPayload: Ref<ProjectMetadataPayload> = ref(
  new ProjectMetadataPayload()
);
const projectBackendTasks = ref<Array<BackendTaskPayload>>([]);
const result = ref<FullResultPayload>(new FullResultPayload());
const resultList = ref<ResultPayload[]>();

defineOptions({
  name: 'ResultsIndexLayout',
});

function queryTaskBackend() {
  if(projectId.value) {
    loadPayloadInstanceFromApi(
      `/project/${projectId.value}/tasks`,
      BackendTaskPayload,
      projectBackendTasks
    );
  }
}

function queryResultListBackend() {
  if(projectId.value) {
    loadPayloadInstanceFromApi(
      `/project/${projectId.value}/list-results`,
      ResultPayload,
      resultList
    );
  }
}

onMounted(() => {
  loadPayloadInstanceFromApi(
    `/result/${resultId}`,
    FullResultPayload,
    result
  ).then(() => {
    if (projectId.value) {
      loadPayloadInstanceFromApi(
        `/project/${projectId.value}`,
        ProjectMetadataPayload,
        projectPayload
      );
      queryTaskBackend();
    }
  });

  queryResultListBackend();
});
useIntervalFn(queryTaskBackend, 2500);
useIntervalFn(queryResultListBackend, 4000);
</script>
<style scoped lang="scss">
.edit-toolbar > * {
  margin-right: 10px;
}
</style>
