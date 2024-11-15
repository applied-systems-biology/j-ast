<template>
  <q-layout view="lHh Lpr lFf">
    <q-header>
      <q-toolbar>
        <q-toolbar-title class="row items-center q-gutter-sm">
          <HeaderLogoButtonComponent />
          <div>/</div>
          <q-skeleton v-if="!projectInfo.name" type="text" style="width: 200px" />
          <router-link style="text-decoration: none; color: inherit;" v-else :to="`/project/${imageInfo.projectId}`">{{ projectInfo.name }}</router-link>
          <div>/</div>
          <q-skeleton v-if="!imageInfo.fileName" type="text" style="width: 200px" />
          <div v-else>{{ imageInfo.fileName }}</div>
<!--          <q-btn-group flat>-->
<!--            <q-btn flat @click="editProjectName">-->
<!--              <q-icon name="edit" />-->
<!--            </q-btn>-->
<!--            <q-btn flat @click="deleteProject">-->
<!--              <q-icon name="delete" />-->
<!--            </q-btn>-->
<!--          </q-btn-group>-->
        </q-toolbar-title>
        <LoginButtonComponent/>
      </q-toolbar>
    </q-header>
    <q-page-container>
      <q-page padding>
        <ProjectImageAnnotationEditor />
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import LoginButtonComponent from "components/AuthManagerComponent.vue";
import HeaderLogoButtonComponent from "components/HeaderLogoButtonComponent.vue";
import { onMounted, Ref, ref } from 'vue';
import { ImagePayload, ProjectMetadataPayload } from 'src/types/common';
import { api } from 'boot/axios';
import { plainToInstance } from 'class-transformer';
import { useRoute } from 'vue-router';
import ProjectImageAnnotationEditor from 'components/ProjectImageAnnotationEditor.vue';

const $route = useRoute()
const imageId = $route.params.imageId

defineOptions({
  name: 'DefaultLayout'
});

const imageInfo: Ref<ImagePayload> = ref(new ImagePayload());
const projectInfo: Ref<ProjectMetadataPayload> = ref(
  new ProjectMetadataPayload()
);

function reloadProjectInfo() {
  api.get(`/image/${imageId}`).then((response) => {
    imageInfo.value = plainToInstance(ImagePayload, response.data);
    api.get<ProjectMetadataPayload>(`/project/${imageInfo.value.projectId}`).then((response) => {
      projectInfo.value = plainToInstance(ProjectMetadataPayload, response.data);
    });
  })

}

onMounted(() => {
  reloadProjectInfo();
});

</script>
