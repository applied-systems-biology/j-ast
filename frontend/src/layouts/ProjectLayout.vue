<template>
  <q-layout view="lHh Lpr lFf">
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
        <q-btn flat round dense icon="menu"/>
        <q-toolbar-title>
          Toolbar
        </q-toolbar-title>
        <q-btn flat round dense icon="more_vert"/>
      </q-toolbar>
    </q-header>
    <q-page-container>
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
