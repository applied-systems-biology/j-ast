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
          <q-btn flat @click="newProject">Start a new project</q-btn>
        </q-card-actions>
      </q-card>
    </div>
    <div class="row q-gutter-md">
      <q-skeleton class="project-item" type="rect"/>
      <q-btn class="project-item" size="lg" icon="add" color="green" outline no-caps @click="newProject">New project</q-btn>
    </div>
  </q-page>
</template>
<script setup lang="ts">

import {useQuasar} from "quasar";

const $q = useQuasar()

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
    console.log(data)
  }).onCancel(() => {
  }).onDismiss(() => {
  })
}
</script>
<style scoped>
.project-item {
  width: 12rem;
  height: 12rem;
}
</style>
