<template>
  <q-dialog ref="dialogRef" @hide="onDialogHide">
    <q-card class="q-dialog-plugin">
      <q-card-section>
        <div class="text-h6">Create project</div>
      </q-card-section>
      <q-separator/>
      <q-card-section>
        <div class="text-bold">
          <q-icon name="help"/>
          Info
        </div>
        <div class="q-mb-md">
          Please set the name of the project and determine if your project
          consists of single images that need to be analyzed independently or if
          you need to organize your images into timelines.
          <strong>The project type can be changed at any point.</strong>
        </div>
      </q-card-section>
      <q-separator/>
      <q-card-section class="q-gutter-sm">
        <q-input v-model="payload.name" class="q-mb-md" filled label="Name"/>
        <div class="text-bold">Project type</div>
        <q-btn-toggle
            no-caps no-wrap
            v-model="payload.viewMode"
            :options="projectTypes"
            filled
            label="Project type"
        />
      </q-card-section>
      <q-separator/>
      <q-card-section>
        <div class="q-mb-md">
          If you have an existing J-AST project, you can provide it here to
          automatically import its content into the newly created project.
        </div>
        <q-file
            v-model="payload.projectArchiveFile"
            accept="application/zip"
            bottom-slots
            filled
        >
          <template v-slot:prepend>
            <q-icon name="cloud_upload" @click.stop.prevent/>
          </template>
          <template v-slot:append>
            <q-icon
                class="cursor-pointer"
                name="close"
                @click.stop.prevent="payload.projectArchiveFile = null"
            />
          </template>
          <template v-slot:hint> Optional *.jast.zip</template>
        </q-file>
      </q-card-section>
      <q-separator/>
      <q-card-actions align="right">
        <q-btn color="blue-grey" label="Cancel" no-caps no-wrap @click="onDialogCancel"/>
        <q-btn
            :disabled="!payload.name" color="green"
            label="OK"
            no-caps
            no-wrap
            @click="onOKClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>
<script lang="ts" setup>
import {useDialogPluginComponent} from 'quasar';
import {ref} from 'vue';
import {CreateProjectRequest,} from 'src/types/project';
import {ViewMode} from 'src/types/view';

const projectTypes = [
  {label: 'Timeline', value: ViewMode.Timeline, icon: 'fa-solid fa-timeline'},
  {label: 'Single images', value: ViewMode.Grid, icon: 'fa-solid fa-grip'},
];

const payload = ref<CreateProjectRequest>(new CreateProjectRequest());

defineEmits([
  // REQUIRED; need to specify some events that your
  // component will emit through useDialogPluginComponent()
  ...useDialogPluginComponent.emits,
]);

const {dialogRef, onDialogHide, onDialogOK, onDialogCancel} =
    useDialogPluginComponent();

function onOKClick() {
  onDialogOK(payload.value);
}
</script>
<style lang="scss" scoped>
.q-dialog-plugin {
  width: 800px;
  max-width: 50vw;
}
</style>
