<template>
  <q-layout view="hHh lpR fFf">
    <q-header>
      <q-toolbar>
        <q-toolbar-title class="row items-center q-gutter-md">
          <HeaderLogoButtonComponent/>
          <div>/</div>
          <q-skeleton v-if="!projectName" type="text" style="width: 200px"/>
          <div v-else>{{ projectName }}</div>
          <q-btn flat @click="editProjectName">
            <q-icon name="edit"/>
          </q-btn>
        </q-toolbar-title>
        <LoginButtonComponent/>
      </q-toolbar>
      <q-toolbar class="bg-primary text-white">
        <ToggleButton not-selected-icon="upload" selected-icon="close" class="bg-secondary" v-model="drawerLeft">Upload</ToggleButton>
      </q-toolbar>
    </q-header>
    <q-drawer elevated side="left" bordered v-model="drawerLeft">
      <ImageUploaderComponent/>
    </q-drawer>
    <q-page-container>
      <q-page padding>
        <q-card class="my-card bg-blue-grey-4 text-white">
          <q-card-section>
            <div class="text-h6">Unsorted images</div>
          </q-card-section>

          <q-card-section>
            <div class="data-list-row">
              <draggable class="draggable" v-model="list" @change="log">
                <transition-group name="fade">
                  <q-btn v-for="element in list" :key="element.id" no-caps class="bg-blue-grey-2 text-black shadow-3 item">
                    <div class="content">
                      <q-img class="thumbnail" :src="dummyImage" fit="contain"/>
                      <div class="label">
                        {{ element.name }}
                      </div>
                    </div>
                  </q-btn>
                </transition-group>
              </draggable>
            </div>
          </q-card-section>

          <q-separator dark />

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
import LoginButtonComponent from "components/LoginButtonComponent.vue";
import HeaderLogoButtonComponent from "components/HeaderLogoButtonComponent.vue";
import {Ref, ref} from "vue";
import ToggleButton from "components/ToggleButton.vue";
import ImageUploaderComponent from "components/ImageUploaderComponent.vue";
import {useQuasar} from "quasar";
import { VueDraggableNext as draggable } from "vue-draggable-next";
import dummyImage from "assets/etest.png"

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
const drawerLeft: Ref<boolean> = ref(false)
const projectName: Ref<string> = ref("")

const list = ref([
  { name: 'John', id: 1 },
  { name: 'Joao', id: 2 },
  { name: 'Jean', id: 3 },
  { name: 'Gerard', id: 4 },
])

function log(event : DragEvent) {
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
      model: projectName.value,
      type: 'text'
    },
    cancel: true,
    persistent: true
  }).onOk((data : string) => {
    projectName.value = data
    // console.log('>>>> OK, received', data)
  }).onCancel(() => {
    // console.log('>>>> Cancel')
  }).onDismiss(() => {
    // console.log('I am triggered on both OK and Cancel')
  })
}

function autoSortImages() {
  $q.notify({
    type: 'negative',
    message: 'This function is currently not available.'
  })
}


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

.data-list-row .draggable {
  display: flex;
  flex-direction: row;
  gap: 1rem;
}

.data-list-row .thumbnail {
  width: 5rem;
  height: 5rem;
}

.data-list-row .content {
  display: flex;
  flex-direction: column;
}
</style>
