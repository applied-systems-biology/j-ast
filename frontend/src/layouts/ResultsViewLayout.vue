<!--
  - Copyright (c) 2026.
  -
  - Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
  - https://www.leibniz-hki.de/en/applied-systems-biology.html
  - HKI-Center for Systems Biology of Infection
  - Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
  - Adolf-Reichwein-Straße 23, 07745 Jena, Germany
  -
  - The project code is licensed under MIT.
  - See the LICENSE file provided with the code for the full license.
  -->

<template>
  <q-layout view="hHh lpR fFf" class="h-vh100">
    <q-header>
      <q-toolbar>
        <q-toolbar-title class="row items-center q-gutter-sm">
          <HeaderLogoButtonComponent/>
          <div>/</div>
          <q-skeleton
              v-if="!projectPayload.name"
              style="width: 200px"
              type="text"
          />
          <router-link
              v-else
              :to="`/project/${projectId}`"
              style="text-decoration: underline; color: inherit"
          >{{ projectPayload.name }}
          </router-link>
          <div>/</div>
          <router-link
              :to="`/results/list/${projectId}`"
              style="text-decoration: underline; color: inherit"
          >Results
          </router-link>
          <div>/</div>
          <q-skeleton
              v-if="!resultPayload"
              style="width: 200px"
              type="text"
          />
          <div v-else>{{ resultPayload.name }}</div>
        </q-toolbar-title>
        <UserManagerComponent/>
        <DocumentationComponent/>
      </q-toolbar>
      <q-toolbar class="bg-primary text-white edit-toolbar">
        <q-btn-dropdown color="green" icon="download" label="Download" no-caps no-wrap>
          <q-list>
            <q-item v-close-popup clickable @click="downloadZip('/')">
              <q-item-section>
                <q-item-label>Download everything (*.zip)</q-item-label>
              </q-item-section>
            </q-item>
            <q-item v-close-popup clickable @click="downloadZip('/' + resultPath)">
              <q-item-section>
                <q-item-label>Download current folder (*.zip)</q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
        </q-btn-dropdown>
      </q-toolbar>
    </q-header>
    <q-drawer
        ref="sidebar"
        :model-value="true"
        class="q-pa-sm q-gutter-sm"
        elevated
        side="left"
    >
      <q-tree
          :nodes="treeNodes"
          node-key="path"
          v-model:selected="selected"
          v-model:expanded="expanded"
          selected-color="primary"
          @update:selected="(p) => { if (p) navigateToFolder(p) }"
      >
        <!-- optional: customize how each node row looks -->
        <template #default-header="prop">
          <div class="row items-center no-wrap q-gutter-sm">
            <q-icon name="folder" />
            <div class="ellipsis">{{ prop.node.label }}</div>
          </div>
        </template>
      </q-tree>
    </q-drawer>
    <q-page-container>
      <q-page padding>
        <q-table ref="table" :columns="filesViewColumns" :pagination="filesViewPagination"
                 :rows="vfsCurrentDirectoryItems" :rows-per-page-options="[0]" :virtual-scroll-sticky-size-start="48" class="file-view-table"
                 row-key="key"
                 virtual-scroll
                 @row-click="onRowClick">
          <template v-slot:body-cell-thumbnail="props">
            <q-td :props="props">
              <ResultItemThumbnailComponent v-if="props.row.id >= 0" :result-item="props.row"/>
              <q-icon v-else-if="props.row.id == -2" class="thumbnail" color="blue" name="fa-solid fa-arrow-up"
                      size="xl"/>
              <q-icon v-else class="thumbnail" color="blue" name="fa-solid fa-folder" size="xl"/>
            </q-td>
          </template>
          <template v-slot:body-cell-actions="props">
            <q-td :props="props">
              <div v-if="props.row.id >= 0" class="q-gutter-sm">
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

<script lang="ts" setup>
import HeaderLogoButtonComponent from 'components/layout/HeaderLogoButtonComponent.vue';
import UserManagerComponent from 'components/layout/UserManagerComponent.vue';
import {useRoute, useRouter} from 'vue-router';
import {ComponentPublicInstance, computed, onBeforeUnmount, onMounted, ref, Ref, useTemplateRef, watch} from 'vue';
import {ProjectMetadataPayload} from 'src/types/project';
import {downloadFromApi, loadPayloadInstanceFromApi} from 'src/types/common';
import {FullResultPayload, generateAndDownloadResultsZip, ResultItemPayload, showResultItem} from 'src/types/results';
import {formatFileSize, sortPathsByHierarchy} from "src/types/utils";
import {QSpinnerHourglass, QTableColumn, QTreeNode, useQuasar} from "quasar";
import ResultItemThumbnailComponent from "components/results/ResultItemThumbnailComponent.vue";
import {sendFailureNotification} from "src/types/notification";
import DocumentationComponent from "components/layout/DocumentationComponent.vue";

const filesViewPagination = {
  rowsPerPage: 0
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

type DirNode = QTreeNode & {
  path: string
  children?: DirNode[]
}

function normalizePath(p: string): string {
  if (!p) return '/'
  // ensure leading slash
  let s = p.startsWith('/') ? p : `/${p}`
  // collapse repeated slashes
  s = s.replace(/\/{2,}/g, '/')
  // remove trailing slash except root
  if (s.length > 1 && s.endsWith('/')) s = s.slice(0, -1)
  return s
}

function buildDirectoryTree(paths: string[]): DirNode[] {
  const root: DirNode = {
    label: '/',
    path: '/',
    // node-key must be unique -> we’ll use full path as key
    // (QTree uses `node[nodeKey]` internally; we set nodeKey="path")
    children: []
  }

  // quick lookup: path -> node
  const byPath = new Map<string, DirNode>()
  byPath.set('/', root)

  // sort to ensure parents are created before children
  const sorted = [...new Set(paths.map(normalizePath))].sort((a, b) => a.localeCompare(b))

  for (const full of sorted) {
    if (full === '/') continue

    const parts = full.split('/').filter(Boolean) // "folder1", "subfolder"
    let currentPath = ''
    let parent = root

    for (let i = 0; i < parts.length; i++) {
      const part = parts[i]
      currentPath = currentPath + '/' + part // builds "/folder1", "/folder1/subfolder", ...

      let node = byPath.get(currentPath)
      if (!node) {
        node = {
          label: part,
          path: currentPath,
          children: []
        }
        byPath.set(currentPath, node)
        parent.children ||= []
        parent.children.push(node)
      }

      parent = node
    }
  }

  // Optional: sort children alphabetically
  const sortRec = (n: DirNode) => {
    if (n.children?.length) {
      n.children.sort((a, b) => String(a.label).localeCompare(String(b.label)))
      n.children.forEach(sortRec)
    }
  }
  sortRec(root)

  return [root]
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

const treeNodes = computed<DirNode[]>(() => buildDirectoryTree(directoryList.value))

// selection: store the selected node key (we use full path)
const selected = ref<string | null>(null)

// expand control (optional)
const expanded = ref<string[]>(['/']) // start with root expanded

// keep selection in sync with your existing active logic:
watch(
    () => '/' + resultPath.value,   // whatever `resultPath` is in your component
    (activePath) => {
      selected.value = normalizePath(activePath)
    },
    { immediate: true }
)

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
  } else {
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
  const toDownload: Array<ResultItemPayload> = []
  for (const item of resultPayload.value.items) {
    const displayPath = "/" + item.path;
    if (displayPath.startsWith(path)) {
      toDownload.push(item)
    }
  }
  if (toDownload.length == 0) {
    sendFailureNotification("Nothing to download.")
    return
  }
  let downloadSizeBytes = 0
  for (const resultItem of toDownload) {
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
    })

    generateAndDownloadResultsZip(toDownload, path, resultPayload.value.name, (percentage, info) => {
      dialog.update({
        message: `${percentage}% ${info}`
      })
    }, () => shouldCancel.value)
        .finally(() => {
          if (!shouldCancel.value) {
            dialog.hide()
          }
        })

  })
}

// Table height sync
const sidebar = useTemplateRef<ComponentPublicInstance>('sidebar')
const table = useTemplateRef<ComponentPublicInstance>('table')

function getDrawerAsideEl(): HTMLElement | null {
  const root = sidebar.value?.$el as HTMLElement | undefined
  if (!root) return null
  // root is .q-drawer-container; the real drawer is the aside
  return root.querySelector('aside.q-drawer') as HTMLElement | null
}

function getTableRootEl(): HTMLElement | null {
  return (table.value?.$el as HTMLElement | undefined) ?? null
}

function syncHeight() {

  const aside = getDrawerAsideEl()
  const tbl = getTableRootEl()
  if (!aside || !tbl) return

  const h = aside.getBoundingClientRect().height - 48
  tbl.style.height = `${Math.floor(h)}px`
}

let ro: ResizeObserver | null = null

function createResizeObserver() {
  ro = new ResizeObserver(syncHeight)

  const drawerEl = sidebar.value?.$el as HTMLElement | undefined
  if (drawerEl) ro.observe(drawerEl)

  window.addEventListener('resize', syncHeight, { passive: true })
}

// Mount
onMounted(() => {

  // Load payloads
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

  // Handle table size sync
  syncHeight()
  createResizeObserver()
});

onBeforeUnmount(() => {
  ro?.disconnect()
  window.removeEventListener('resize', syncHeight)
})
</script>
<style lang="scss" scoped>
.directory-list {
  display: flex;
  flex-direction: column;
}

.thumbnail {
  width: 100px;
  height: 100px;
}

.file-view-table {
  height: 80vh;
}

</style>
