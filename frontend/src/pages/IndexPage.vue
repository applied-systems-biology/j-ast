<template>
  <q-page padding>
    <div class="q-mb-lg">
      <q-card class="my-card bg-secondary text-white">
        <q-card-section>
          <div class="text-h6">Welcome to J-AST</div>
        </q-card-section>

        <q-card-section>
        </q-card-section>

        <q-separator dark />

        <q-card-actions>
          <q-btn :disable="!authStore.isLoggedIn" icon="add" flat @click="newProject">Start a new project</q-btn>
          <q-btn :disable="!authStore.isLoggedIn" icon="refresh" flat @click="refreshProjectList">Refresh</q-btn>
          <q-chip outline color="white" square icon="warning" v-if="!authStore.isLoggedIn">
            You are currently not logged in
          </q-chip>
        </q-card-actions>
      </q-card>
    </div>
    <div class="row q-gutter-md" v-if="authStore.isLoggedIn">
      <q-skeleton v-if="projectList == null" class="project-item" type="rect"/>
      <q-btn v-for="project in projectList" :key="project.id" color="blue-grey-2" class="project-item" size="lg" outline no-caps @click="openProject(project.id)" push>
        <div class="row items-start no-wrap full-width text-blue-grey">
          <q-icon left name="folder" />
        </div>
        <div class="row items-start no-wrap full-width text-blue-grey">
          <div class="text-center ellipsis" >
            {{ project.name }}
          </div>
        </div>
        <div class="row items-start no-wrap full-width text-caption text-blue-grey">
          <div class="flex column">
            <div class="text-left ellipsis">
              ID {{ project.id }}
            </div>
            <div class="text-left">
              Owner: {{ project.owner || "Admin" }}
            </div>
          </div>
        </div>
      </q-btn>
      <q-btn class="project-item" size="lg" icon="add" color="green" outline no-caps @click="newProject">New project</q-btn>
    </div>
  </q-page>
</template>
<script setup lang="ts">

import {useQuasar} from "quasar";
import {useAuthStore} from "stores/auth-store";
import {ref} from "vue";
import {storeToRefs} from "pinia";
import {api} from "boot/axios";
import {useWatchInterval} from "../composables/UseWatchInterval";
import {CreateEditProjectRequest, ProjectMetadataPayload} from "src/types/common";
import {useRouter} from "vue-router";

const $q = useQuasar()
const authStore = useAuthStore()
const router = useRouter()
const { isLoggedIn } = storeToRefs(authStore)
const projectList = ref<Array<ProjectMetadataPayload> | null>()

/**
 * Creates a new project
 */
function newProject() {
  $q.dialog({
    title: 'Create new project',
    message: 'Please enter the name of the newly created project',
    prompt: {
      model: "",
      type: 'text'
    },
    cancel: true,
    persistent: true
  }).onOk((data : string) => {
    api.post("/new-project", { name: data } as CreateEditProjectRequest)
      .then((result) => {
        const info = result.data as ProjectMetadataPayload;
        $q.notify({
          type: 'positive',
          message: `Created new project "${info.name}"`
        })
        router.push(`/project/${info.id}`)
      })
      .catch((reason) => {
        console.log(reason);
        $q.notify({
          type: 'negative',
          message: "Unable to create project!"
        })
      })
  }).onCancel(() => {
  }).onDismiss(() => {
  })
}

/**
 * Populate/clear the project list
 */
function refreshProjectList() {
  projectList.value = null
  if(isLoggedIn.value) {
    api.get("/list-projects")
      .then((result) => {
        projectList.value = result.data
      })
  }
  else {
    projectList.value = null
  }
}

function openProject(id : number) {
  router.push(`/project/${id}`)
}

/**
 * Watch for auto-updating the project list
 */
useWatchInterval(isLoggedIn, () => {
 refreshProjectList()
})

</script>
<style scoped>
.project-item {
  width: 12rem;
  height: 12rem;
}
</style>
