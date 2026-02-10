<template>
  <q-btn no-wrap color="cyan" @click="goToTasksView">
    <q-spinner v-if="numRunning > 0" class="q-mr-md" />
    <q-icon name="check" v-if="numRunning == 0" class="q-mr-md" />
    <div v-if="numRunning == 0">All tasks finished</div>
    <div v-if="numRunning > 1">{{ numRunning }} tasks are running</div>
    <div v-if="numRunning == 1">1 task is running</div>
    <q-badge floating class="container-badge">
      <q-badge color="red" v-if="numFailures > 0">{{ numFailures }}</q-badge>
      <q-badge color="green" v-if="numSuccesses > 0"
        >{{ numSuccesses }}
      </q-badge>
    </q-badge>
  </q-btn>
</template>
<script setup lang="ts">
import { BackendTaskPayload, TaskStatus } from 'src/types/backendTasks';
import { computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import {
  sendFailureNotification,
  sendInfoNotification,
  sendSuccessNotification,
} from 'src/types/notification';

defineOptions({
  name: 'ProjectTasksLayout',
});

const $router = useRouter();
const projectBackendTasks = defineModel<BackendTaskPayload[]>();
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
<style scoped lang="scss">
.container-badge {
  background: transparent;
  top: -10px;
  right: -10px;
  gap: 2px;
}
</style>
