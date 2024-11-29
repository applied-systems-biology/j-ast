<template>
  <q-dialog ref="dialogRef" @hide="onDialogHide" maximized>
    <q-layout view="hHh lpR fFf" container class="layout">
      <q-header>
        <q-toolbar>
          <q-space/>
          <q-btn icon="close" flat @click="onDialogOK" />
        </q-toolbar>
      </q-header>
      <q-page-container>
        <q-page class="flex column">
          <ImageViewer class="viewer" :image-backend-url="props.imageBackendUrl"/>
        </q-page>
      </q-page-container>
    </q-layout>
  </q-dialog>
</template>
<script setup lang="ts">
import {useDialogPluginComponent} from 'quasar';
import ImageViewer from "components/dataViewers/ImageViewer.vue";

const props = defineProps<{
  imageBackendUrl: string;
}>()

defineEmits([
  ...useDialogPluginComponent.emits,
]);

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } =
  useDialogPluginComponent();


</script>

<style scoped lang="scss">
.stage-container {
  box-shadow: 0 1px 5px rgba(0, 0, 0, 0.2), 0 2px 2px rgba(0, 0, 0, 0.14),
  0 3px 1px -2px rgba(0, 0, 0, 0.12);
  flex-grow: 1;
  overflow: hidden;
  height: 0;
  margin-left: 8px !important;
}

.tool-control {
  margin-bottom: 2em;
}

.tool-control-badge {
  display: flex;
  flex-direction: row;
  gap: 3px;
  height: 3em;

  .label {
    flex-grow: 1;
  }
}


.layout {
  height: 100vh;
  background: white;
}

.viewer {
  flex-grow: 1;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

</style>
<style lang="scss">
.konvajs-content {
  cursor: crosshair;
}
</style>
