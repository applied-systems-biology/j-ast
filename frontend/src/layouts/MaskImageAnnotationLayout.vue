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
        <q-btn-toggle color="blue-grey" toggle-color="green" v-model="currentAnnotationColorId" :options="annotationColors">
          <q-tooltip>Determines whether the foreground or the background is drawn</q-tooltip>
        </q-btn-toggle>
        <q-btn-toggle color="blue-grey" toggle-color="green" v-model="currentAnnotationToolId" :options="annotationTools">
          <q-tooltip>The tool to draw the foreground/background</q-tooltip>
        </q-btn-toggle>
        <q-btn color="blue-grey" label="Tools" icon="fa-solid fa-gear">
          <q-menu>
            <q-list style="min-width: 100px">
              <q-item clickable v-close-popup @click="resetView">
                <q-item-section avatar>
                  <q-icon name="fa-solid fa-expand"/>
                </q-item-section>
                <q-item-section>Reset view</q-item-section>
              </q-item>
              <q-separator />
              <q-item clickable v-close-popup @click="clearAnnotation">
                <q-item-section avatar>
                  <q-icon name="fa-solid fa-eraser"/>
                </q-item-section>
                <q-item-section>Clear</q-item-section>
              </q-item>
              <q-item clickable v-close-popup @click="restoreSavedState">
                <q-item-section avatar>
                  <q-icon name="fa-solid fa-undo"/>
                </q-item-section>
                <q-item-section>Restore saved state</q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </q-btn>

      </q-toolbar>
    </q-header>
    <q-page-container>
      <q-page padding class="flex column q-gutter-sm">
        <ProjectImageAnnotationEditor ref="editorComponent" v-model="annotationPayload" v-model:tool-color="currentAnnotationColorId" v-model:tool-id="currentAnnotationToolId"/>
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import LoginButtonComponent from "components/AuthManagerComponent.vue";
import HeaderLogoButtonComponent from "components/HeaderLogoButtonComponent.vue";
import {computed, onMounted, Ref, ref, useTemplateRef} from 'vue';
import {AssayType, ImagePayload, MaskImageAnnotationPayload, ProjectMetadataPayload} from 'src/types/common';
import {api} from 'boot/axios';
import {plainToInstance} from 'class-transformer';
import {useRoute} from 'vue-router';
import ProjectImageAnnotationEditor from 'components/MaskImageAnnotationEditor.vue';
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
  { label: "", value: "pan", icon: "fa-solid fa-hand" },
  { label: "", value: "draw", icon: "fa-solid fa-pencil" },
  { label: "", value: "polygon", icon: "fa-solid fa-draw-polygon" },
  { label: "", value: "line", icon: "fa-solid fa-slash" },
  { label: "", value: "fill", icon: "fa-solid fa-fill-drip" },
]
const annotationColors = [
  { label: "Foreground", value: "#FFFFFF", icon: "fa-solid fa-square" },
  { label: "Background", value: "#000000", icon: "fa-solid fa-eraser" },
]
const currentAnnotationToolId = ref("draw");
const currentAnnotationColorId = ref("#FFFFFF");

const imagePayload: Ref<ImagePayload> = ref(new ImagePayload());
const projectPayload: Ref<ProjectMetadataPayload> = ref(
  new ProjectMetadataPayload()
);
const annotationPayload : Ref<MaskImageAnnotationPayload> = ref(new MaskImageAnnotationPayload());

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

function resetView() {
  editorComponent.value?.resetLocationAndZoom()
}

function saveAndUpload() {
  editorComponent.value?.saveImage()
}

function restoreSavedState() {
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

function clearAnnotation() {
  $q.dialog({
    title: 'Clear annotation',
    message: 'Do you really want to erase the annotation?',
    cancel: {
      label: 'No',
    },
    ok: {
      label: 'Yes',
      color: 'red',
    },
    persistent: true,
  }).onOk(() => {
    editorComponent.value?.clear()
  });
}

function queryFromBackend() {
  api.get(`/mask-image-annotation/${imageId}/${annotationTypeId}`).then(response => {
    annotationPayload.value = plainToInstance(MaskImageAnnotationPayload, response.data)

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
