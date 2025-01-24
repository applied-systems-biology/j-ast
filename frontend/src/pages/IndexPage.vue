<template>
  <q-page class="page-container">
    <q-toolbar v-if="authStore.isLoggedIn" class="bg-primary edit-toolbar">
      <q-btn :disable="!authStore.isLoggedIn || !canAddProject" icon="add" color="green" @click="newProject">New project</q-btn>
      <q-btn :disable="!authStore.isLoggedIn" icon="refresh" color="blue-5" @click="refreshProjectList">Refresh</q-btn>
    </q-toolbar>
    <q-scroll-area class="q-pa-md project-list" v-if="authStore.isLoggedIn">
      <div class="row q-gutter-md" >
        <q-skeleton v-if="projectList == null" class="project-item" type="rect"/>
        <q-btn v-for="project in projectList" :key="project.id" color="blue-grey-2" class="project-item" size="lg" outline
               no-caps @click="openProject(project.id)" push>
          <div class="row items-start no-wrap full-width text-blue-grey">
            <q-icon left name="folder"/>
          </div>
          <div class="row items-start no-wrap full-width text-blue-grey">
            <div class="text-center ellipsis">
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
        <q-btn v-if="canAddProject" class="project-item" size="lg" icon="add" color="green" outline no-caps @click="newProject">New project
        </q-btn>
        <q-btn v-else-if="authStore.isGuest" class="project-item" size="lg" icon="block" color="red" outline no-caps @click="sendFailureNotification('Too many projects. Please contact the administrator if you want more.')">Project limit reached
        </q-btn>
      </div>
    </q-scroll-area>
    <q-scroll-area class="project-list" v-if="!authStore.isLoggedIn">
      <div class="bg-blue-grey-2 q-pt-lg q-pb-lg hero">
        <img class="q-mt-lg q-mb-lg logo" :src="logo" alt="J-AST logo" />
        <div class="text-h5 text-weight-light">Your tool for managing, annotating, and analyzing disk diffusion assays and E-tests</div>
      </div>
      <div class="q-pa-md q-gutter-sm features-list">
        <q-card class="feature-list-item">
          <q-card-section horizontal>
            <q-img
              class="col-5"
              :src="heroUpload"
            />
            <q-card-section class="col">
              <div class="text-overline">Feature</div>
              <div class="text-h5 q-mt-sm q-mb-xs">Upload your data</div>
              <div>
                Use the uploader component to store your data into a J-AST project.
                <p>You can later at any time download your data as *.zip.</p>
              </div>
            </q-card-section>
          </q-card-section>
        </q-card>
        <q-card class="feature-list-item">
          <q-card-section horizontal>
            <q-img
              class="col-5"
              :src="heroArrangeTimeSeriues"
            />
            <q-card-section class="col">
              <div class="text-overline">Feature</div>
              <div class="text-h5 q-mt-sm q-mb-xs">Annotate and arrange</div>
              <div>
                Annotate your data with essential metadata by either doing the work manually or
                using our automated tool that extracts the information from the file name.
                <p>Then you can proceed to either arrange your time lines manually via drag and drop or use
                  the included automated tool that utilizes the metadata.</p>
              </div>
            </q-card-section>
          </q-card-section>
        </q-card>
        <q-card class="feature-list-item">
          <q-card-section horizontal>
            <q-img
              class="col-5"
              :src="heroProcess"
            />
            <q-card-section class="col">
              <div class="text-overline">Feature</div>
              <div class="text-h5 q-mt-sm q-mb-xs">Automated processing</div>
              <div>
                Select which data to process and run a variety of automated image processing and analysis algorithms
                directly from within J-AST.
              </div>
            </q-card-section>
          </q-card-section>
        </q-card>
        <q-card class="feature-list-item">
          <q-card-section horizontal>
            <q-img
              class="col-5"
              :src="heroInteractiveAnnotation"
            />
            <q-card-section class="col">
              <div class="text-overline">Feature</div>
              <div class="text-h5 q-mt-sm q-mb-xs">Manually guide the automated analysis</div>
              <div>
                The automated detection algorithms don't work or yield unsatisfactory results?
                Don't worry - all annotations can be edited manually directly within J-AST.
              </div>
            </q-card-section>
          </q-card-section>
        </q-card>
        <q-card class="feature-list-item">
          <q-card-section horizontal>
            <q-img
              class="col-5"
              :src="heroResultsBrowser"
            />
            <q-card-section class="col">
              <div class="text-overline">Feature</div>
              <div class="text-h5 q-mt-sm q-mb-xs">Review results</div>
              <div>
                Annotate your data with essential metadata by either doing the work manually or
                using our automated tool that extracts the information from the file name.
                <p>Then you can proceed to either arrange your time lines manually via drag and drop or use
                the included automated tool that utilizes the metadata.</p>
              </div>
            </q-card-section>
          </q-card-section>
        </q-card>
      </div>
    </q-scroll-area>

  </q-page>
</template>
<script setup lang="ts">

import {useQuasar} from "quasar";
import {useAuthStore} from "stores/auth-store";
import { computed, ref } from 'vue';
import {storeToRefs} from "pinia";
import {api} from "boot/axios";
import {useWatchInterval} from "../composables/UseWatchInterval";
import {useRouter} from "vue-router";
import {plainToInstance} from 'class-transformer';
import {sendFailureNotification, sendSuccessNotification} from "src/types/notification";
import { CreateEditProjectRequest, ProjectMetadataPayload } from 'src/types/project';
import logo from "assets/logo-j-ast-full.svg"
import heroUpload from "assets/hero/hero-upload.png"
import heroArrangeTimeSeriues from "assets/hero/hero-arrange-time-series.png"
import heroProcess from "assets/hero/hero-process.png"
import heroInteractiveAnnotation from "assets/hero/hero-interactive-annotation.png"
import heroResultsBrowser from "assets/hero/hero-results-browser.png"

const $q = useQuasar()
const authStore = useAuthStore()
const router = useRouter()
const {isLoggedIn} = storeToRefs(authStore)
const projectList = ref<ProjectMetadataPayload[] | null>()
const canAddProject = computed(() => {
  if(authStore.isLoggedIn && projectList.value) {
    if(authStore.isGuest) {
      return projectList.value?.length < authStore.limits.guestMaxProjects
    }
    else {
      return true
    }
  }
  else {
    return false
  }
})

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
  }).onOk((data: string) => {
    api.post("/new-project", {name: data} as CreateEditProjectRequest)
      .then((result) => {
        const info = plainToInstance(ProjectMetadataPayload, result.data)
        sendSuccessNotification(`Created new project "${info.name}"`)
        router.push(`/project/${info.id}`)
      })
      .catch((reason) => {
        console.log(reason);
        sendFailureNotification("Unable to create project!")
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
  if (isLoggedIn.value) {
    api.get<ProjectMetadataPayload[]>("/list-projects")
      .then((result) => {
        projectList.value = result.data
      })
  } else {
    projectList.value = null
  }
}

function openProject(id: number) {
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

.page-container {
  display: flex;
  flex-direction: column;
}

.project-item {
  width: 12rem;
  height: 12rem;
}

.project-list {
  flex-grow: 1;
  height: 200px;
}

.hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-grow: 1;
  .logo {
    width: 50vw;
    max-width: 1024px;
  }
}

.features-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  .feature-list-item {
    width: 50vw;
  }
}
</style>
