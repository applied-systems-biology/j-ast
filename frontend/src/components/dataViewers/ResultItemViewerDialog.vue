<template>
  <q-dialog ref="dialogRef" @hide="onDialogHide" maximized>
    <q-layout view="hHh lpR fFf" container class="layout">
      <q-header>
        <q-toolbar-title class="row items-center q-gutter-sm">
          <div class="q-ml-md">{{ props.resultItem.path }} / {{ props.resultItem.name }}</div>
          <q-space />
          <q-btn icon="close" flat @click="onDialogOK" />
        </q-toolbar-title>
      </q-header>
      <q-page-container>
        <q-page class="flex column">
          <ImageViewer
            v-if="props.resultItem.getVisualizationType() == ResultItemType.Image"
            class="viewer"
            :image-backend-url="props.resultItem.getVisualizationUrl()"
            :filename="props.resultItem.name"
          />
          <q-banner v-else rounded class="bg-blue-grey text-white q-ma-md">
            Currently, the web application does not know how to display {{ props.resultItem.name }}.<br/>
            Please download the file and view it on your computer.
            <template v-slot:action>
              <q-btn color="green" label="Download" @click="downloadResultItem"/>
            </template>
          </q-banner>
        </q-page>
      </q-page-container>
    </q-layout>
  </q-dialog>
</template>
<script setup lang="ts">
import { useDialogPluginComponent } from 'quasar';
import ImageViewer from 'components/dataViewers/ImageViewer.vue';
import { ResultItemPayload, ResultItemType } from 'src/types/results';
import { downloadFromApi } from "src/types/common";

const props = defineProps<{
  resultItem: ResultItemPayload;
}>();

defineEmits([...useDialogPluginComponent.emits]);

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } =
  useDialogPluginComponent();

function downloadResultItem() {
  downloadFromApi(
    `/result-item/${props.resultItem.id}/raw`,
    props.resultItem.name
  );
}

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
