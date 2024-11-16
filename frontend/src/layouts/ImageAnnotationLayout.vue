<template>
  <q-layout view="lHh Lpr lFf">
    <q-header>
      <q-toolbar>
        <q-toolbar-title class="row items-center q-gutter-sm">
          <HeaderLogoButtonComponent />
          <div>/</div>
          <q-skeleton v-if="!projectPayload.name" type="text" style="width: 200px" />
          <router-link style="text-decoration: underline; color: inherit;" v-else :to="`/project/${imagePayload.projectId}`">{{ projectPayload.name }}</router-link>
          <div>/</div>
          <q-skeleton v-if="!imagePayload.fileName" type="text" style="width: 200px" />
          <div v-else>{{ imagePayload.fileName }}</div>
          <div>/</div>
          <div>{{ annotationName }}</div>
          <q-btn color="green" icon="upload" size="lg" @click="saveAndUpload">Save annotation</q-btn>
        </q-toolbar-title>
        <LoginButtonComponent/>
      </q-toolbar>
      <q-toolbar class="bg-primary text-white edit-toolbar">
        <q-btn-toggle color="blue-grey" toggle-color="green" v-model="currentAnnotationToolId" :options="annotationTools"/>
        <q-btn color="red-4" icon="undo" @click="resetAnnotation">Reset</q-btn>
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
import {useQuasar} from "quasar";

const $q = useQuasar()
const $route = useRoute()
const imageId = $route.params.imageId
const annotationTypeId = $route.params.annotationTypeId
const editorComponent = useTemplateRef<typeof ProjectImageAnnotationEditor>("editorComponent")

defineOptions({
  name: 'ImageAnnotationLayout'
});

const annotationTools = [
  { label: "Draw", value: "draw", icon: "fa-solid fa-pencil" },
  { label: "Erase", value: "erase", icon: "fa-solid fa-eraser" },
]
const currentAnnotationToolId = ref("draw");

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

function resetAnnotation() {
  $q.dialog({
    title: 'Restore from saved annotation',
    message: 'Do you really want to reset the annotation from the saved state?',
    cancel: {
      label: 'No',
    },
    ok: {
      label: 'Yes',
      color: 'red',
    },
    persistent: true,
  }).onOk(() => {
    editorComponent.value?.queryFromBackend()
  });
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
<style scoped lang="scss">
.edit-toolbar > * {
  margin-right: 10px;
}
</style>
