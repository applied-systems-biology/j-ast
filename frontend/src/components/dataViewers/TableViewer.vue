<template>
  <div class="flex column">
    <q-toolbar class="bg-primary text-white edit-toolbar">
      <q-btn label="Download" icon="fa-solid fa-download" color="green" @click="download"/>
      <ToggleButton selected-icon="fa-regular fa-square-check" not-selected-icon="fa-regular fa-square" v-model="enhancedView" label="Enhanced display"/>
    </q-toolbar>
  </div>
  <q-scroll-area class="row col-grow q-pa-sm" style="height: 400px; max-width: 100vw;">
    <q-table :rows="tableData" :pagination="tablePagination">
      <template v-slot:body-cell="props">
        <q-td :props="props">
          <div v-if="!enhancedView">
            {{ props.value }}
          </div>
          <div class="row q-gutter-md" style="align-items: center" v-else>
            <ProjectImageIdThumbnailComponent v-if="props.col.field.startsWith('#ImageId')" :image-id="props.value" />
            <div>
              {{ props.value }}
            </div>
          </div>
        </q-td>
      </template>
    </q-table>
  </q-scroll-area>
<!--  <div class="row col-grow q-pa-sm" style="width: calc(100vw - 10px); border: 1px solid red;">-->

<!--  </div>-->
</template>
<script setup lang="ts">
import Papa from "papaparse"
import { onMounted, ref, watch } from "vue";
import { useQuasar } from 'quasar';
import { api } from 'boot/axios';
import { sendFailureNotification } from 'src/types/notification';
import { downloadFromApi, ensureExtension } from 'src/types/common';
import ToggleButton from "components/utils/ToggleButton.vue";
import ProjectImageIdThumbnailComponent from "components/results/ProjectImageIdThumbnailComponent.vue";

const $q = useQuasar()

const props = defineProps<{
  tableBackendUrl: string;
  filename: string;
}>()
const enhancedView = ref(true)
const tablePagination = {
  rowsPerPage: 100
}
const tableData = ref<Array<any>>([])

function queryFromBackend() {
  if(props.tableBackendUrl) {
    $q.loading.show({
      message: 'Loading table ...',
    });
    api.get(props.tableBackendUrl, {
      responseType: 'blob',
    })
      .then(response => {
        Papa.parse(response.data, {
          header: true,
          skipEmptyLines: true,
          complete: (results) => {
            tableData.value = results.data;
          },
          error: (error) => {
            sendFailureNotification("Error loading table");
            console.error(error);
          },
        });
      })
      .catch(() => {
        sendFailureNotification("Unable to load table")
      })
      .finally(() => {
        $q.loading.hide()
      })
  }
}

function download() {
  downloadFromApi(
    props.tableBackendUrl,
    ensureExtension(props.filename, ['.csv'])
  );
}

onMounted(() => {
  queryFromBackend();
});
watch(() => props.tableBackendUrl, queryFromBackend)

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


</style>
<style lang="scss">
.konvajs-content {
  cursor: crosshair;
}
</style>

