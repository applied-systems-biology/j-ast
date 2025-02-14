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
        <DocumentationComponent/>
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
        <q-table :rows="resultsTableRows" :columns="resultsTableColumns" :pagination="filesViewPagination"
                 @row-click="onRowClick" row-key="key">
          <template v-slot:body-cell-thumbnail="props">
            <q-td :props="props">
              <q-icon class="thumbnail" name="fa-solid fa-folder" color="blue" size="xl"/>
              <q-badge align="top" color="green" v-if="!props.row.viewed">New</q-badge>
            </q-td>
          </template>
          <template v-slot:body-cell-actions="props">
            <q-td :props="props">
              <div class="q-gutter-sm" v-if="props.row.id >= 0">
                <q-btn icon="search" @click.stop="goToResult(props.row.id)"/>
                <q-btn icon="delete" @click.stop="deleteResult(props.row)"/>
              </div>
            </q-td>
          </template>
        </q-table>
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import HeaderLogoButtonComponent from 'components/layout/HeaderLogoButtonComponent.vue';
import AuthManagerComponent from 'components/layout/AuthManagerComponent.vue';
import { useRoute, useRouter } from 'vue-router';
import { computed, onMounted, ref, Ref } from 'vue';
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
import { QTableColumn } from 'quasar';
import DocumentationComponent from "components/layout/DocumentationComponent.vue";

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

const filesViewPagination = {
  rowsPerPage: 100
}

const resultsTableColumns : QTableColumn[] = [
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
    name: "description",
    label: "Description",
    field: "description",
    sortable: true,
    align: 'left',
  },
  {
    name: "createdAt",
    label: "Created at",
    field: "createdAt",
    sortable: true,
    align: 'left',
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

const resultsTableRows = computed(() => {
    if(!resultList.value) {
      return []
    }
    return [...resultList.value].sort((a, b) =>
      new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
  }
);

function onRowClick(evt: any, row: ResultPayload) {
  goToResult(row.id)
}

function goToResult(id: number) {
  $router.push(`/results/view/${id}`);
}

function deleteResult(result: ResultPayload) {
  onDialogYes(
    'Delete result',
    `Do you really want to delete the result "${result.name}"?`
  ).then(() => {
    console.log(result);
    console.log(`/result/${result.id}/delete`)
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
