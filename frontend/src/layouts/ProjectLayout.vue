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
      <ProjectImageEditor v-model="selectedImage"/>
    </q-drawer>
    <q-page-container>
      <q-page padding class="q-gutter-sm">
        <!-- Unsorted row -->
        <q-card class="bg-indigo text-white">
          <q-card-section>
            <div class="text-h6">Unsorted images</div>
          </q-card-section>
          <q-card-section>
            <q-scroll-area class="data-list-row">
              <draggable
                class="draggable"
                :list="projectImages"
                :group="{ name: 'g1' }">
                <ProjectImageButton v-for="image in projectImages"
                                    :key="image.id"
                                    :current-image="image"
                                    :selected-image-id="selectedImageId"
                                    @clicked="onImageClicked"/>
              </draggable>

            </q-scroll-area>
          </q-card-section>
          <q-separator dark/>
          <q-card-actions>
            <q-btn flat @click="autoSortImages">Auto-sort</q-btn>
          </q-card-actions>
        </q-card>

        <!-- Existing rows -->
        <!--        <q-card v-for="rowIndex in maxImageRow" :key="rowIndex" class="bg-blue-grey-4 text-white">-->
        <!--          <q-card-section>-->
        <!--            <div class="text-h6">{{ rowIndex }}</div>-->
        <!--          </q-card-section>-->

        <!--          <q-card-section>-->
        <!--            <q-scroll-area class="data-list-row">-->
        <!--              <draggable-->
        <!--                class="draggable"-->
        <!--                v-model="projectImages"-->
        <!--                :group="{ name: 'images', pull: true, put: true }">-->
        <!--                <transition-group name="fade">-->
        <!--                </transition-group>-->
        <!--              </draggable>-->
        <!--            </q-scroll-area>-->
        <!--          </q-card-section>-->
        <!--        </q-card>-->

                <!-- New row -->
                <q-card class="bg-blue-grey-4 text-white">
                  <q-card-section>
<!--                    <div class="text-h6">{{ maxImageRow + 1 }}</div>-->
                  </q-card-section>

                  <q-card-section>
                    <q-scroll-area class="data-list-row">
                      <draggable
                        class="draggable"
                        :list="projectImages"
                        :group="{ name: 'g1' }">
                        <transition-group name="fade">
                        </transition-group>
                      </draggable>
                    </q-scroll-area>
                  </q-card-section>
                </q-card>
<!--                <div class="flex m-10">-->
<!--                  <draggable class="dragArea list-group w-full" :list="projectImages">-->
<!--                    <div-->
<!--                      class="list-group-item bg-gray-300 m-1 p-3 rounded-md text-center"-->
<!--                      v-for="element in projectImages"-->
<!--                      :key="'vdb' + element.id"-->
<!--                    >-->
<!--                      {{ element.fileName }}-->
<!--                    </div>-->
<!--                  </draggable>-->
<!--                </div>-->
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
import {CreateEditProjectRequest, ImagePayload, ProjectMetadataPayload} from "src/types/common";
import ProjectImageButton from "components/ProjectImageButton.vue";
import ProjectImageEditor from "components/ProjectImageEditor.vue";

// type DragEvent = {
//   to: HTMLElement; // list, in which moved element
//   from: HTMLElement; // previous list
//   item: HTMLElement; // dragged element
//   clone: HTMLElement;
//   oldIndex?: number; // old index within parent
//   newIndex?: number; // new index within parent
//   oldDraggableIndex?: number; // old index within parent, only counting draggable elements
//   newDraggableIndex?: number; // new index within parent, only counting draggable elements
//   pullMode?: "clone" | true | false; // Pull mode if dragging into another sortable, otherwise undefined
// };


const $q = useQuasar()
const $route = useRoute()
const router = useRouter()
const drawerLeft: Ref<boolean> = ref(false)
const projectName = computed(() => projectInfo.value?.name ?? undefined)
const projectId = $route.params.id
const projectInfo: Ref<ProjectMetadataPayload | null> = ref(null)
const projectImages = ref<ImagePayload[]>([])
const projectImagesById = ref<Map<number, ImagePayload>>(new Map())
const selectedImageId = ref<number>(-1)

// Components
// const unsortedDraggableRowElement = useTemplateRef<HTMLElement>("unsortedDraggableRowElementRef")

// Computed values
const drawerRight = computed(() => selectedImageId.value >= 0)
const selectedImage = computed(() => projectImages.value.find(image => image.id === selectedImageId.value))
// const maxImageRow = computed(() => Math.max(...projectImages.value.map((image) => image.groupRow), 0))
// const unsortedImages = computed(() => projectImages.value.filter((image) => image.groupRow < 0 || image.groupColumn < 0))

// function handleDrag(evt: DragEvent) {
//   console.log('Item moved:', evt.item);
//   console.log('From list:', evt.from);
//   console.log('To list:', evt.to);
//   console.log('To list:', evt.to.getAttribute("data-target-row"));
//   console.log("From index:", evt.oldDraggableIndex)
//   console.log("To index:", evt.newDraggableIndex)
//   // console.log(unsortedDraggableRowElement.value)
//   // console.log(evt.to == unsortedDraggableRowElement.value)
// }

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
        projectInfo.value = response.data as ProjectMetadataPayload
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
      projectInfo.value = response.data as ProjectMetadataPayload
    })
  api.get(`/project/${projectId}/list-images`)
    .then(response => {
      projectImages.value = response.data as ImagePayload[]
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
  height: calc(var(--size) + 8rem);
}

.data-list-row .draggable {
  display: flex;
  flex-direction: row;
  gap: 1rem;
}

.draggable {
  background: green;
  height: calc(var(--size) + 8rem);
}
</style>
