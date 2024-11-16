<template>
  <q-layout view="lHh Lpr lFf">
    <q-header>
      <q-toolbar>
        <q-toolbar-title class="row items-center q-gutter-sm">
          <HeaderLogoButtonComponent />
          <div>/</div>
          <q-skeleton v-if="!projectPayload.name" type="text" style="width: 200px" />
          <router-link style="text-decoration: none; color: inherit;" v-else :to="`/project/${imagePayload.projectId}`">{{ projectPayload.name }}</router-link>
          <div>/</div>
          <q-skeleton v-if="!imagePayload.fileName" type="text" style="width: 200px" />
          <div v-else>{{ imagePayload.fileName }}</div>
          <div>/</div>
          <div>{{ annotationName }}</div>
          <q-btn color="green" icon="save" size="lg" @click="saveAndUpload">Save annotation</q-btn>
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
      <q-toolbar class="bg-primary text-white edit-toolbar">

      </q-toolbar>
    </q-header>
    <q-page-container>
      <q-page padding>
        <ProjectImageAnnotationEditor ref="editorComponent" v-model="annotationPayload"/>
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import LoginButtonComponent from "components/AuthManagerComponent.vue";
import HeaderLogoButtonComponent from "components/HeaderLogoButtonComponent.vue";
import {computed, onMounted, Ref, ref, useTemplateRef} from 'vue';
import {AssayType, ImagePayload, ImageAnnotationPayload, ProjectMetadataPayload} from 'src/types/common';
import {api} from 'boot/axios';
import {plainToInstance} from 'class-transformer';
import {useRoute} from 'vue-router';
import ProjectImageAnnotationEditor from 'components/ProjectImageAnnotationEditor.vue';

const $route = useRoute()
const imageId = $route.params.imageId
const annotationTypeId = $route.params.annotationTypeId
const editorComponent = useTemplateRef<typeof ProjectImageAnnotationEditor>("editorComponent")

defineOptions({
  name: 'DefaultLayout'
});

const imagePayload: Ref<ImagePayload> = ref(new ImagePayload());
const projectPayload: Ref<ProjectMetadataPayload> = ref(
  new ProjectMetadataPayload()
);
const annotationPayload : Ref<ImageAnnotationPayload> = ref(new ImageAnnotationPayload());

const annotationName = computed(() => {
  switch (annotationTypeId) {
    case "plate":
      return "Plate"
    case "strip-disk":
      if(imagePayload.value.assayType == AssayType.ETest) {
        return "ETest strip"
      }
      else if(imagePayload.value.assayType == AssayType.DDA) {
        return "DDA disk"
      }
      break
    case "zoi-shape":
      return "ZOI"
  }
  return annotationTypeId;
})

function saveAndUpload() {
  editorComponent.value?.saveImage()
}

function queryFromBackend() {
  api.get(`/image-annotation/${imageId}/${annotationTypeId}`).then(response => {
    annotationPayload.value = plainToInstance(ImageAnnotationPayload, response.data)

    // Load info about the image and the

  })
  api.get(`/image/${imageId}`).then((response) => {
    imagePayload.value = plainToInstance(ImagePayload, response.data);
    api.get<ProjectMetadataPayload>(`/project/${imagePayload.value.projectId}`).then((response) => {
      projectPayload.value = plainToInstance(ProjectMetadataPayload, response.data);
    });
  })
}


onMounted(() => {
  queryFromBackend();
});

</script>
