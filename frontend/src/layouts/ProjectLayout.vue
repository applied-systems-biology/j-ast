<template>
  <q-layout view="hHh lpR fFf">
    <q-header>
      <q-toolbar>
        <q-toolbar-title class="row items-center q-gutter-sm">
          <HeaderLogoButtonComponent/>
          <div>/</div>
          <q-skeleton v-if="!projectName" type="text" style="width: 200px"/>
          <div v-else>{{ projectName }}</div>
          <q-btn-group flat>
            <q-btn flat @click="editProjectName">
              <q-icon name="edit"/>
            </q-btn>
            <q-btn flat @click="deleteProject">
              <q-icon name="delete"/>
            </q-btn>
          </q-btn-group>

        </q-toolbar-title>
        <LoginButtonComponent/>
      </q-toolbar>
      <q-toolbar class="bg-primary text-white">
        <ToggleButton not-selected-icon="upload" selected-icon="close" class="bg-secondary q-mr-sm"
                      v-model="drawerLeft">Upload
        </ToggleButton>
        <q-btn icon="delete" color="red-5" v-if="selectedImageId >= 0" @click="deleteSelectedImage">Delete</q-btn>
      </q-toolbar>
    </q-header>
    <q-drawer elevated side="left" bordered v-model="drawerLeft">
      <ImageUploaderComponent :project-id="projectId[0]" @finished="reloadProjectInfo"/>
    </q-drawer>
    <q-drawer elevated side="right" bordered v-model="drawerRight" class="q-pa-sm q-gutter-sm">
      <q-input :model-value="selectedImage?.fileName" filled label="File name"/>
      <q-input :model-value="selectedImage?.experiment" filled label="Experiment"/>
      <q-input :model-value="selectedImage?.sample" filled label="Sample"/>
      <q-input :model-value="selectedImage?.timePoint" filled label="Time point"/>
    </q-drawer>
    <q-page-container>
      <q-page padding>
        <q-card class="my-card bg-blue-grey-4 text-white">
          <q-card-section>
            <div class="text-h6">Unsorted images</div>
          </q-card-section>

          <q-card-section>
            <q-scroll-area class="data-list-row">
              <draggable class="draggable" v-model="projectImages" @change="log">
                <transition-group name="fade">
                  <q-btn v-for="element in projectImages"
                         :key="element.id"
                         no-caps class="shadow-3 item text-black"
                         :color="selectedImageId === element.id ? 'green-3' : 'blue-grey-2'"
                         @click="onImageClicked(element.id)">
                    <div class="content">
                      <q-img class="thumbnail" :src="element.thumbnailData" fit="contain"/>
                      <div class="label text-left">
                        <div class="text-caption ellipsis">
                          {{ element.fileName }}
                        </div>
                        <div v-if="element.experiment" class="text-caption ellipsis text-blue-grey">
                          {{ element.experiment }}
                        </div>
                        <div v-if="!element.experiment" class="text-caption ellipsis text-blue-grey">
                          <i>&lt;No experiment&gt;</i>
                        </div>
                        <div v-if="element.sample" class="text-caption ellipsis text-blue-grey">
                          {{ element.sample }}
                        </div>
                        <div v-if="!element.sample" class="text-caption ellipsis text-blue-grey">
                          <i>&lt;No sample&gt;</i>
                        </div>
                        <div v-if="element.timePoint" class="text-caption ellipsis text-blue-grey">
                          {{ element.timePoint }}
                        </div>
                        <div v-if="!element.timePoint" class="text-caption ellipsis text-blue-grey">
                          <i>&lt;No time point&gt;</i>
                        </div>
                      </div>
                    </div>
                  </q-btn>
                </transition-group>
              </draggable>
            </q-scroll-area>
          </q-card-section>

          <q-separator dark/>

          <q-card-actions>
            <q-btn flat @click="autoSortImages">Auto-sort</q-btn>
          </q-card-actions>
        </q-card>

      </q-page>
    </q-page-container>
    <q-footer>
      <ImprintComponent/>
    </q-footer>
  </q-layout>
</template>

<script setup lang="ts">
import ImprintComponent from "components/ImprintComponent.vue";
import LoginButtonComponent from "components/AuthManagerComponent.vue";
import HeaderLogoButtonComponent from "components/HeaderLogoButtonComponent.vue";
import {onMounted, Ref, ref, computed} from "vue";
import ToggleButton from "components/ToggleButton.vue";
import ImageUploaderComponent from "components/ImageUploaderComponent.vue";
import {useQuasar} from "quasar";
import {VueDraggableNext as draggable} from "vue-draggable-next";
import {useRoute, useRouter} from "vue-router";
import {api} from "boot/axios";
import {CreateEditProjectRequest, ImageInfoMessage, ProjectInfoMessage} from "src/types/common";

type DragEvent = {
  to: HTMLElement
  from: HTMLElement
  dragged: HTMLElement
  draggedRect: DOMRect
  related: HTMLElement
  relatedRect: DOMRect
  willInsertAfter: boolean
}

const $q = useQuasar()
const $route = useRoute()
const router = useRouter()
const drawerLeft: Ref<boolean> = ref(false)
const projectName = computed(() => projectInfo.value?.name ?? undefined)
const projectId = $route.params.id
const projectInfo: Ref<ProjectInfoMessage | null> = ref(null)
const projectImages = ref<ImageInfoMessage[]>([])
const selectedImageId = ref<number>(-1)

const drawerRight = computed(() => selectedImageId.value >= 0)
const selectedImage = computed(() => projectImages.value.find(image => image.id === selectedImageId.value))

function log(event: DragEvent) {
  console.log(event)
}

defineOptions({
  name: 'ProjectLayout'
});

function editProjectName() {
  $q.dialog({
    title: 'Edit project name',
    message: 'Please enter a new project name',
    prompt: {
      model: projectName.value || "",
      type: 'text'
    },
    cancel: true,
    persistent: true
  }).onOk((data: string) => {
    api.post(`/project/${projectId}/edit`, {name: data} as CreateEditProjectRequest)
      .then(response => {
        projectInfo.value = response.data as ProjectInfoMessage
      })
  })
}

function deleteProject() {
  $q.dialog({
    title: 'Delete project',
    message: 'Do your really want to delete the current project?',
    cancel: {
      label: "No"
    },
    ok: {
      label: "Yes",
      color: "red"
    },
    persistent: true
  }).onOk(() => {
    api.post(`/project/${projectId}/delete`, {})
      .then(() => {
        router.push("/")
      })
  })
}

function deleteSelectedImage() {
  if (selectedImage.value) {
    $q.dialog({
      title: 'Delete image',
      message: `Do your really want to delete the image '${selectedImage.value.fileName}'?`,
      cancel: {
        label: "No"
      },
      ok: {
        label: "Yes",
        color: "red"
      },
      persistent: true
    }).onOk(() => {
      api.post(`/image/${selectedImageId.value}/delete`, {})
        .then(() => {
          reloadProjectInfo()
        })
    })
  }
}

function autoSortImages() {
  $q.notify({
    type: 'negative',
    message: 'This function is currently not available.'
  })
}

function reloadProjectInfo() {
  api.get(`/project/${projectId}`)
    .then(response => {
      projectInfo.value = response.data as ProjectInfoMessage
    })
  api.get(`/project/${projectId}/list-images`)
    .then(response => {
      projectImages.value = response.data as ImageInfoMessage[]
      // Un-select the image
      if (!selectedImage.value) {
        selectedImageId.value = -1
      }
    })
}

function onImageClicked(imageId: number) {
  if (selectedImageId.value == imageId) {
    selectedImageId.value = -1
  } else {
    selectedImageId.value = imageId
  }
}

onMounted(() => {
  reloadProjectInfo()
})

</script>
<style scoped>
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.5s;
}

.fade-enter, .fade-leave-to {
  opacity: 0;
}

.fade-move {
  transition: transform 0.5s;
}

.data-list-row {
  --size: 10rem;
  height: calc(var(--size) + 6rem);
}

.data-list-row .draggable {
  display: flex;
  flex-direction: row;
  gap: 1rem;
}

.data-list-row .thumbnail {
  width: var(--size);
  height: var(--size);
}

.data-list-row .content {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: calc(var(--size) - 1rem);
}

.data-list-row .text-caption {
  width: calc(var(--size) - 1rem);
}

.data-list-row .active {
  background-color: blue;
}

</style>
