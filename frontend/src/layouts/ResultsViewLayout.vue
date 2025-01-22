<template>
  <q-layout view="hHh lpR fFf">
    <q-header>
      <q-toolbar>
        <q-toolbar-title class="row items-center q-gutter-sm">
          <HeaderLogoButtonComponent/>
          <div>/</div>
          <q-skeleton
            v-if="!projectPayload.name"
            type="text"
            style="width: 200px"
          />
          <router-link
            style="text-decoration: underline; color: inherit"
            v-else
            :to="`/project/${projectId}`"
          >{{ projectPayload.name }}
          </router-link>
          <div>/</div>
          <router-link
            style="text-decoration: underline; color: inherit"
            :to="`/results/list/${projectId}`"
          >Results
          </router-link>
          <div>/</div>
          <q-skeleton
            v-if="!resultPayload"
            type="text"
            style="width: 200px"
          />
          <div v-else>{{ resultPayload.name }}</div>
        </q-toolbar-title>
        <AuthManagerComponent/>
      </q-toolbar>
      <q-toolbar class="bg-primary text-white edit-toolbar">
        <q-btn-dropdown color="green" icon="download" label="Download">
          <q-list>
            <q-item clickable v-close-popup @click="downloadZip('/')">
              <q-item-section>
                <q-item-label>Download everything (*.zip)</q-item-label>
              </q-item-section>
            </q-item>
            <q-item clickable v-close-popup @click="downloadZip('/' + resultPath)">
              <q-item-section>
                <q-item-label>Download current folder (*.zip)</q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
        </q-btn-dropdown>
      </q-toolbar>
    </q-header>
    <q-drawer
      side="left"
      :model-value="true"
      elevated
      class="q-pa-sm q-gutter-sm"
    >
      <q-list dense padding class="rounded-borders">
        <q-item v-for="directoryPath in directoryList" :key="directoryPath" clickable v-ripple
                @click="navigateToFolder(directoryPath)" :active="('/' + resultPath) == directoryPath">
          <q-item-section avatar>
            <q-icon name="folder"/>
          </q-item-section>
          <q-item-section>
            {{ directoryPath }}
          </q-item-section>
        </q-item>
      </q-list>
    </q-drawer>
    <q-page-container>
      <q-page padding>
        <q-table :rows="vfsCurrentDirectoryItems" :columns="filesViewColumns" :pagination="filesViewPagination"
                 @row-click="onRowClick" row-key="key">
          <template v-slot:body-cell-thumbnail="props">
            <q-td :props="props">
              <ResultItemThumbnailComponent v-if="props.row.id >= 0" :result-item="props.row"/>
              <q-icon v-else-if="props.row.id == -2" class="thumbnail" name="fa-solid fa-arrow-up" color="blue"
                      size="xl"/>
              <q-icon v-else class="thumbnail" name="fa-solid fa-folder" color="blue" size="xl"/>
            </q-td>
          </template>
          <template v-slot:body-cell-actions="props">
            <q-td :props="props">
              <div class="q-gutter-sm" v-if="props.row.id >= 0">
                <q-btn icon="download" @click.stop="downloadResultItem(props.row.id)"/>
                <q-btn icon="search" @click.stop="displayResultItem(props.row.id)"/>
              </div>
            </q-td>
          </template>
          <template v-slot:body-cell-fileSize="props">
            <q-td :props="props">
             <span v-if="props.row.id >= 0">{{ formatFileSize(props.row.size) }}</span>
            </q-td>
          </template>
        </q-table>
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import HeaderLogoButtonComponent from 'components/layout/HeaderLogoButtonComponent.vue';
import AuthManagerComponent from 'components/layout/AuthManagerComponent.vue';
import {useRoute, useRouter} from 'vue-router';
import {computed, onMounted, ref, Ref} from 'vue';
import {ProjectMetadataPayload} from 'src/types/project';
import {downloadFromApi, loadPayloadInstanceFromApi} from 'src/types/common';
import { FullResultPayload, generateAndDownloadResultsZip, ResultItemPayload, showResultItem } from 'src/types/results';
import { formatFileSize, sortPathsByHierarchy } from "src/types/utils";
import {QSpinnerHourglass, QTableColumn, useQuasar} from "quasar";
import ResultItemThumbnailComponent from "components/results/ResultItemThumbnailComponent.vue";
import {sendFailureNotification} from "src/types/notification";

const filesViewPagination = {
  rowsPerPage: 100
}
const filesViewColumns: QTableColumn[] = [
  {
    name: "thumbnail",
    label: "",
    field: "id",
    sortable: false,
    align: 'center',
    style: 'width: 120px; height: 120px;',
  },
  {
    name: "fileName",
    label: "File Name",
    field: "name",
    sortable: true,
    align: 'left',
  },
  {
    name: "fileSize",
    label: "Size",
    field: "size",
    sortable: true,
    align: 'left',
  },
  {
    name: "fileType",
    label: "Type",
    field: "type",
    sortable: true,
    align: 'left',
  },
  {
    name: "actions",
    label: "",
    field: "id",
    sortable: false,
    align: 'right',
  }
]

interface VfsEntry {
  id: number;
  name: string;
  type: string;
  content: ResultItemPayload | string;
  key: string;
  size: number;
}

const $q = useQuasar()
const $route = useRoute();
const $router = useRouter()
const resultId = $route.params.id;
const resultPath = computed(() => $route.params.path || "");
const projectId = computed(() => resultPayload.value.projectId ? resultPayload.value.projectId.toString() : '');
const projectPayload: Ref<ProjectMetadataPayload> = ref(
  new ProjectMetadataPayload()
);
const resultPayload = ref<FullResultPayload>(new FullResultPayload());
const directoryList = computed(() => {
  const allPaths = new Set<string>();
  for (const resultItem of resultPayload.value.items) {
    let path = resultItem.path || "";
    if (!path.startsWith("/")) {
      path = `/${path}`
    }
    allPaths.add(path);
  }
  return sortPathsByHierarchy(allPaths)
})
const vfsCurrentDirectoryItems = computed(() => {
  const itemList: Array<VfsEntry> = []
  const allPaths = new Set<string>();
  const currentPath = resultPath.value + ""
  for (const resultItem of resultPayload.value.items) {
    const candidatePath = resultItem.path
    if (resultItem.path == currentPath) {
      itemList.push({
        id: resultItem.id,
        name: resultItem.name,
        type: resultItem.type,
        content: resultItem,
        key: resultItem.id + "",
        size: resultItem.size,
      });
    } else if (!allPaths.has(candidatePath)) {
      let success = false
      if (currentPath == "") {
        // If we are at the root, add if candidate is not "" and has depth 1
        if (candidatePath != "" && !candidatePath.includes("/")) {
          success = true
        }
      } else {
        // If we are deeper, ensure that the number of / is exactly 1 higher
        if (candidatePath.split("/").length == currentPath.split("/").length + 1) {
          success = true
        }
      }
      if (success) {
        itemList.push({
          id: -1,
          name: resultItem.path,
          type: "Directory",
          content: resultItem.path,
          key: resultItem.path,
          size: 0
        });
        allPaths.add(resultItem.path);
      }
    }
  }

  if (currentPath != "") {
    const parentPath = currentPath.substring(0, currentPath.lastIndexOf("/"));
    // Add "Parent folder"
    itemList.push({
      id: -2,
      name: "Parent directory",
      type: "Directory",
      content: parentPath,
      key: parentPath,
      size: 0
    });
  }

  const collator = new Intl.Collator(undefined, {numeric: true, sensitivity: 'base'});
  itemList.sort((a, b) => {
    // Compare by type: directories first
    if (a.type === 'Directory' && b.type !== 'Directory') return -1;
    if (a.type !== 'Directory' && b.type === 'Directory') return 1;

    // If both are the same type, sort by name in natural order
    return collator.compare(a.name, b.name);
  });

  return itemList
})

defineOptions({
  name: 'ResultsViewLayout',
});

function navigateToFolder(path: string) {
  if (path.startsWith("/")) {
    path = path.substring(1);
  }
  $router.push({path: `/results/view/${resultId}/${path}`});
}

function onRowClick(evt: any, row: VfsEntry) {
  if (row.type == "Directory") {
    if (typeof row.content === "string") {
      navigateToFolder(row.content)
    }
  }
  else {
    displayResultItem(row.id)
  }
}

function downloadResultItem(id: number) {
  const resultItem = resultPayload.value.items.findLast(v => v.id == id)
  if (resultItem) {
    downloadFromApi(
      `/result-item/${id}/raw`,
      resultItem.name
    );
  } else {
    sendFailureNotification("Unable to retrieve result item with id " + id)
  }
}

function displayResultItem(id: number) {
  const resultItem = resultPayload.value.items.findLast(v => v.id == id)
  if (resultItem) {
    showResultItem(resultItem)
  } else {
    sendFailureNotification("Unable to retrieve result item with id " + id)
  }
}

function downloadZip(path: string) {
  const toDownload : Array<ResultItemPayload> = []
  for(const item of resultPayload.value.items) {
    const displayPath = "/" + item.path;
    if(displayPath.startsWith(path)) {
      toDownload.push(item)
    }
  }
  if(toDownload.length == 0) {
    sendFailureNotification("Nothing to download.")
    return
  }
  let downloadSizeBytes = 0
  for(const resultItem of toDownload) {
    downloadSizeBytes += resultItem.size
  }
  $q.dialog({
    title: 'Download results',
    message: `You are about to download ${toDownload.length} files (${formatFileSize(downloadSizeBytes)}).<br/>Do you want to continue?<br/><br/>Please note that due how the ZIP file is created, your computer needs at least ${formatFileSize(downloadSizeBytes)} of free RAM space.`,
    html: true,
    cancel: true,
    persistent: true
  }).onOk(() => {
    const shouldCancel = ref<boolean>(false);
    const dialog = $q.dialog({
      title: 'Downloading results ...',
      message: 'Preparing ...',
      progress: {
        spinner: QSpinnerHourglass,
      },
      persistent: true,
      ok: false,
      cancel: true,
    })
    dialog.onCancel(() => {
      shouldCancel.value = true
      dialog.hide()
    })

    generateAndDownloadResultsZip(toDownload, path, resultPayload.value.name, (percentage, info) => {
      dialog.update({
        message: `${percentage}% ${info}`
      })
    }, () => shouldCancel.value)
      .finally(() => {
        dialog.hide()
      })

  })
}

onMounted(() => {
  loadPayloadInstanceFromApi(
    `/result/${resultId}`,
    FullResultPayload,
    resultPayload
  ).then(() => {
    if (projectId.value) {
      loadPayloadInstanceFromApi(
        `/project/${projectId.value}`,
        ProjectMetadataPayload,
        projectPayload
      );
    }
  });
});
</script>
<style scoped lang="scss">
.directory-list {
  display: flex;
  flex-direction: column;
}

.thumbnail {
  width: 100px;
  height: 100px;
}
</style>
