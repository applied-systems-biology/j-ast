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
    <q-drawer elevated side="left" overlay bordered v-model="drawerLeft">
      <ImageUploaderComponent/>
    </q-drawer>
    <q-page-container>
      bbb
      <EditProjectNameDialog v-model="editProjectNameDialogOpen" @onProjectNameChanged="onProjectNameChanged"/>
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
import EditProjectNameDialog from "components/EditProjectNameDialog.vue";
import ToggleButton from "components/ToggleButton.vue";
import ImageUploaderComponent from "components/ImageUploaderComponent.vue";

const drawerLeft: Ref<boolean> = ref(false)
const projectName: Ref<string> = ref("")
const editProjectNameDialogOpen = ref(false)

defineOptions({
  name: 'ProjectLayout'
});

function editProjectName() {
  editProjectNameDialogOpen.value = true;
}

function onProjectNameChanged(newValue: string) {
  projectName.value = newValue
}

</script>
