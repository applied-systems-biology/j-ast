<template>
  <q-btn color="cyan" @click="goToTasksView">
    <q-spinner v-if="numRunning > 0" class="q-mr-md"/>
    <q-icon name="check" v-if="numRunning == 0" class="q-mr-md"/>
    <div v-if="numRunning == 0">All tasks finished</div>
    <div v-if="numRunning > 1">{{ numRunning }} tasks are running</div>
    <div v-if="numRunning == 1">1 task is running</div>
    <q-badge floating class="container-badge">
        <q-badge color="red" v-if="numFailures > 0">{{ numFailures }}</q-badge>
        <q-badge color="green" v-if="numSuccesses > 0">{{ numSuccesses }}</q-badge>
    </q-badge>

  </q-btn>

</template>
<script setup lang="ts">
import {BackendTaskPayload, TaskStatus} from "src/types/backendTasks";
import {computed} from "vue";
import {useRouter} from "vue-router";

defineOptions({
  name: 'ProjectTasksLayout'
});

const $router = useRouter()
const projectBackendTasks = defineModel<BackendTaskPayload[]>()
const numSuccesses = computed(() => {
  let count = 0;
  if (projectBackendTasks.value) {
    for (const task of projectBackendTasks.value) {
      if (task.status == TaskStatus.Successful) {
        count++;
      }
    }
  }
  return count
})
const numFailures = computed(() => {
  let count = 0;
  if (projectBackendTasks.value) {
    for (const task of projectBackendTasks.value) {
      if (task.status == TaskStatus.Failed) {
        count++;
      }
    }
  }
  return count
})
const numRunning = computed(() => {
  let count = 0;
  if (projectBackendTasks.value) {
    for (const task of projectBackendTasks.value) {
      if (task.status == TaskStatus.Running || task.status == TaskStatus.Ready) {
        count++;
      }
    }
  }
  return count
})

const props = defineProps<{
  projectId?: string
}>()

function goToTasksView() {
  $router.push(`/tasks/${props.projectId}`)
}

</script>
<style scoped lang="scss">
.container-badge {
  background: transparent;
  top: -10px;
  right: -10px;
  gap: 2px;
}
</style>
