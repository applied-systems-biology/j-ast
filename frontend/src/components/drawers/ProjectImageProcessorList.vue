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
  <q-input v-model="filter" class="q-mb-md" color="purple" dense filled label="Search ...">
    <template v-slot:append>
      <q-icon v-if="filter !== ''" class="cursor-pointer" name="clear" @click="resetFilter"/>
    </template>
  </q-input>
  <q-scroll-area class="w-100 grow">
    <q-tree
        v-model:expanded="expanded"
        :filter="filter"
        :nodes="nodes"
        no-connectors
        node-key="label"
    >
      <template v-slot:default-header="prop">
        <q-btn v-if="prop.node.action" :disable="hasTaskRunning" :icon="prop.node.icon" :label="prop.node.label"
               align="left"
               class="w-100" dense flat
               no-caps no-wrap @click="prop.node.action">
          <q-tooltip>{{ prop.node.description }}</q-tooltip>
        </q-btn>
        <span v-else>{{ prop.node.label }}</span>
      </template>
    </q-tree>
  </q-scroll-area>
</template>
<script lang="ts" setup>
import {computed, onMounted, ref} from "vue";
import {BackendTaskPayload, BackendTaskTypePayload, doBackendTask} from "src/types/backendTasks";
import {api} from "boot/axios";
import {plainToInstance} from "class-transformer";
import {FrontEndImageProcessor, frontEndImageProcessors} from "src/types/frontendTasks";
import {ViewMode} from "src/types/view";
import {sendFailureNotification, sendSuccessNotification} from "src/types/notification";
import {ProjectImagesPayload} from "src/types/projectImages";

const emit = defineEmits<{
  (e: "onFrontendTaskFinished", task: FrontEndImageProcessor): void;
}>();

const expanded = ref()
const filter = ref('')
const availableBackendTasks = ref<Array<BackendTaskTypePayload>>([]);
const currentViewMode = defineModel<ViewMode>("currentViewMode", {required: true})

const selectedImageIds = defineModel<Array<number>>("selectedImageIds", {required: true})
const projectImages = defineModel<ProjectImagesPayload>("projectImages", {required: true})
const projectBackendTasks = defineModel<BackendTaskPayload[]>('projectBackendTasks', {required: true});
const selectedImages = computed(() =>
    selectedImageIds.value.map((id) => projectImages.value.getImageById(id))
);
const hasTaskRunning = computed(() => {
  if (projectBackendTasks.value) {
    for (const task of projectBackendTasks.value) {
      if (task.isRunning()) {
        return true
      }
    }
    return false
  }
  return true; // Waiting still for info
});


const availableBackendTasksCategories = computed(() => {
  const predefinedOrder = ["Preprocessing", "Plate", "DDA", "E-Test", "Analyze"];
  const result = new Set<string>();
  for (const taskType of availableBackendTasks.value) {
    result.add(taskType.category || '');
  }

  // Convert the Set to an Array and sort
  const predefinedOrderSet = new Set(predefinedOrder);
  const sortedList = Array.from(result).sort((a, b) => {
    const indexA = predefinedOrderSet.has(a) ? predefinedOrder.indexOf(a) : predefinedOrder.length;
    const indexB = predefinedOrderSet.has(b) ? predefinedOrder.indexOf(b) : predefinedOrder.length;
    if (indexA !== indexB) {
      return indexA - indexB; // Sort by predefined order
    }
    return a.localeCompare(b); // Sort alphabetically for items not in the predefined order
  });

  return sortedList.filter((item) => !!item);
});

const nodes = computed(() => {
  const result: any[] = []

  // Add frontend tasks
  for (const tool of frontEndImageProcessors) {
    if (tool.viewMode == undefined || tool.viewMode == currentViewMode.value)
      result.push({
        label: tool.label,
        description: tool.tooltip,
        icon: tool.icon,
        action: () => {
          doFrontEndProcessor(tool)
        }
      })
  }

  // Add backend tasks (categorized)
  for (const category of availableBackendTasksCategories.value) {
    const children = []
    for (const tool of availableBackendTasks.value.filter((task) => task.category == category)) {
      children.push({
        label: tool.name,
        description: tool.shortDescription,
        icon: "fa-solid fa-wand-magic-sparkles",
        action: () => {
          doBackendTaskClicked(tool)
        }
      })
    }
    const categoryNode = {
      label: category,
      children: children
    }

    result.push(categoryNode)
  }

  // Add backend tasks without category
  for (const tool of availableBackendTasks.value.filter((task) => !task.category)) {
    result.push({
      label: tool.name,
      description: tool.shortDescription,
      icon: "fa-solid fa-wand-magic-sparkles",
      action: () => {
        doBackendTaskClicked(tool)
      }
    })
  }

  return result
})

function resetFilter() {
  filter.value = ''
}

function emitOnFrontendTaskFinished(task: FrontEndImageProcessor) {
  emit("onFrontendTaskFinished", task);
}

function doFrontEndProcessor(tool: FrontEndImageProcessor) {
  if (selectedImageIds.value && projectImages.value) {
    tool
        .fn(selectedImages.value, projectImages.value)
        .then((response) => {
          sendSuccessNotification(`Successfully applied "${tool.label}"`);
          if (response.needsUpload) {
            projectImages.value
                .uploadToBackend()
                .then(() => {
                  emitOnFrontendTaskFinished(tool)
                })
                .catch(() =>
                    sendFailureNotification(`Failed to update selected images`)
                );
          } else if (response.needsFullReload) {
            emitOnFrontendTaskFinished(tool)
          }
        })
        .catch(() => {
          sendFailureNotification(`Error while applying "${tool.label}"`);
        })
        .finally(() => {
        });
  }
}

function doBackendTaskClicked(tool: BackendTaskTypePayload) {
  if (selectedImageIds.value && projectImages.value) {
    doBackendTask(
        [...selectedImages.value],
        projectImages.value.projectId,
        tool,
        projectImages.value
    );
  }
}

onMounted(() => {
  api.get<BackendTaskTypePayload[]>(`/task/list-types`).then((response) => {
    availableBackendTasks.value = plainToInstance(
        BackendTaskTypePayload,
        response.data
    );
  });
})
</script>
<style lang="scss" scoped>

</style>
