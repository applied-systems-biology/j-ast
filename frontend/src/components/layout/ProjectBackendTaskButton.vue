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
  <q-btn color="cyan" no-caps no-wrap @click="goToTasksView">
    <q-spinner v-if="numRunning > 0" class="q-mr-md"/>
    <q-icon v-if="numRunning == 0" class="q-mr-md" name="check"/>
    <div v-if="numRunning == 0">Tasks</div>
    <div v-if="numRunning > 1">{{ numRunning }} task is running</div>
    <div v-if="numRunning == 1">1 task is running</div>
    <q-badge class="container-badge" floating>
      <q-badge v-if="numFailures > 0" color="red">{{ numFailures }}</q-badge>
      <q-badge v-if="numSuccesses > 0" color="green"
      >{{ numSuccesses }}
      </q-badge>
    </q-badge>
  </q-btn>
</template>
<script lang="ts" setup>
import {BackendTaskPayload, TaskStatus} from 'src/types/backendTasks';
import {computed, watch} from 'vue';
import {useRouter} from 'vue-router';
import {sendFailureNotification, sendInfoNotification, sendSuccessNotification,} from 'src/types/notification';

defineOptions({
  name: 'ProjectTasksLayout',
});

const $router = useRouter();
const projectBackendTasks = defineModel<BackendTaskPayload[]>({required: true});
const numSuccesses = computed(() => {
  let count = 0;
  if (projectBackendTasks.value) {
    for (const task of projectBackendTasks.value) {
      if (task.status == TaskStatus.Successful) {
        count++;
      }
    }
  }
  return count;
});
const numFailures = computed(() => {
  let count = 0;
  if (projectBackendTasks.value) {
    for (const task of projectBackendTasks.value) {
      if (task.status == TaskStatus.Failed) {
        count++;
      }
    }
  }
  return count;
});
const numRunning = computed(() => {
  let count = 0;
  if (projectBackendTasks.value) {
    for (const task of projectBackendTasks.value) {
      if (
          task.status == TaskStatus.Running ||
          task.status == TaskStatus.Ready
      ) {
        count++;
      }
    }
  }
  return count;
});

const props = defineProps<{
  projectId?: string;
}>();
const emit = defineEmits<{
  (e: "onTaskFinished", task: BackendTaskPayload): void;
  (e: "onTaskFailed", task: BackendTaskPayload): void;
}>();
const lastTaskStates: Record<string, TaskStatus> = {};
let lastTaskStatesInitialized = false;

function goToTasksView() {
  $router.push(`/tasks/${props.projectId}`);
}

function handleNewTaskDetected(task: BackendTaskPayload) {
  sendInfoNotification(
      `Started "${task.name}" on ${task.imageIds.length} images`
  );
}

function handleTaskStatusChanged(
    task: BackendTaskPayload,
    from: TaskStatus,
    to: TaskStatus
) {
  if (from == TaskStatus.Running && to == TaskStatus.Failed) {
    emit("onTaskFailed", task);
    sendFailureNotification(`The task "${task.name}" has failed`);
  } else if (from == TaskStatus.Running && to == TaskStatus.Successful) {
    emit("onTaskFinished", task);
    sendSuccessNotification(`The task "${task.name}" was successful`);
  }
}

watch(projectBackendTasks, () => {
  if (projectBackendTasks.value) {
    if (!lastTaskStatesInitialized) {
      // Do a pure initialization
      for (let task of projectBackendTasks.value) {
        lastTaskStates[task.id.toString()] = task.status;
      }
      lastTaskStatesInitialized = true;
    } else {
      // We do an update and watch for changes
      for (let task of projectBackendTasks.value) {
        if (!lastTaskStates[task.id.toString()]) {
          handleNewTaskDetected(task);
        } else if (lastTaskStates[task.id.toString()] != task.status) {
          handleTaskStatusChanged(
              task,
              lastTaskStates[task.id.toString()],
              task.status
          );
        }
        lastTaskStates[task.id.toString()] = task.status;
      }
    }
  }
});
</script>
<style lang="scss" scoped>
.container-badge {
  background: transparent;
  top: -10px;
  right: -10px;
  gap: 2px;
}
</style>
