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
          <router-link
            style="text-decoration: underline; color: inherit"
            :to="`/results/list/${projectId}`"
          >Results
          </router-link>
        </q-toolbar-title>
        <AuthManagerComponent />
      </q-toolbar>
      <q-toolbar class="bg-primary text-white edit-toolbar">
        <div class="col-grow" />
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
import { FullResultPayload } from 'src/types/results';

const $route = useRoute();
const resultId = $route.params.id;
const projectId = computed(() => result.value.projectId ? result.value.projectId.toString() : '');
const projectPayload: Ref<ProjectMetadataPayload> = ref(
  new ProjectMetadataPayload()
);
const result = ref<FullResultPayload>(new FullResultPayload());

defineOptions({
  name: 'ResultsIndexLayout',
});

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
    }
  });
});
</script>
