<template>
  <q-dialog ref="dialogRef" @hide="onDialogHide">
    <q-card class="q-dialog-plugin">
      <q-card-section>
        <div class="text-h6">Create project</div>
      </q-card-section>
      <q-separator />
      <q-card-section>
        <div class="text-bold">
          <q-icon name="help" />
          Info
        </div>
        <div class="q-mb-md">
          Please set the name of the project and determine if your project consists of single images that need to be analyzed independently or
          if you need to organize your images into time lines.
          <strong>The project type can be changed at any point.</strong>
        </div>
      </q-card-section>
      <q-separator />
      <q-card-section class="q-gutter-sm">
        <q-input class="q-mb-md" v-model="payload.name" label="Name" filled />
        <div class="text-bold">Project type</div>
        <q-btn-toggle v-model="payload.viewMode" :options="projectTypes" label="Project type" filled/>
      </q-card-section>
      <q-separator />
      <q-card-actions align="right">
        <q-btn color="blue-grey" label="Cancel" @click="onDialogCancel" />
        <q-btn color="green" label="OK" @click="onOKClick" :disabled="!payload.name" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>
<script setup lang="ts">
import { useDialogPluginComponent } from 'quasar';
import { ref } from 'vue';
import { CreateEditProjectRequest } from 'src/types/project';
import { ViewMode } from 'src/types/view';

const projectTypes =  [
  {label: 'Timeline', value: ViewMode.Timeline, icon: "fa-solid fa-timeline"},
  {label: 'Single images', value: ViewMode.Grid, icon: "fa-solid fa-grip"},
]

const payload = ref<CreateEditProjectRequest>(new CreateEditProjectRequest())

defineEmits([
  // REQUIRED; need to specify some events that your
  // component will emit through useDialogPluginComponent()
  ...useDialogPluginComponent.emits,
]);

const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } =
  useDialogPluginComponent();

function onOKClick() {
  onDialogOK(payload.value);
}

</script>
<style scoped lang="scss">
.q-dialog-plugin {
  width: 800px;
  max-width: 50vw;
}
</style>
