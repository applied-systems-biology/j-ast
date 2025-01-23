<template>
  <q-dialog v-model="visible" seamless position="bottom">
    <q-card :style="{ width: '500px', maxWidth: '70vw' }">
      <q-card-section class="row no-wrap">
        <div class="col-grow text-bold" >
          Current task progress
        </div>
        <ToggleButton size="sm" flat selected-icon="fa-solid fa-angles-up" not-selected-icon="fa-solid fa-angles-down" v-model="collapsed"/>
      </q-card-section>
      <q-card-section class="log-container" v-if="!collapsed">
        <transition-group name="log" tag="div">
          <div v-for="log in displayedLogs" :key="log.id" class="log-line">
            {{ log.text }}
          </div>
        </transition-group>
      </q-card-section>
      <q-card-section  v-if="!collapsed">
        <q-linear-progress indeterminate />
      </q-card-section>
    </q-card>
  </q-dialog>
</template>
<script setup lang="ts">
import { useIntervalFn } from '@vueuse/core';
import { computed, ref } from 'vue';
import { BackendTaskPayload, TaskStatus } from 'src/types/backendTasks';
import ToggleButton from 'components/utils/ToggleButton.vue';
import { api } from 'boot/axios';
// import { api } from 'boot/axios';

const pollingInterval = 2500;

const visible = ref(false)
const collapsed = ref(false);

// const logs = ref<{ id: number; text: string }[]>([]);
const displayedLogs = ref<{ id: number; text: string }[]>([]);
const maxDisplayedLogs = 5;
let logId = 0;

const projectBackendTasks = defineModel<BackendTaskPayload[]>('backendTasks');

const currentTask = computed(() => {
  if(projectBackendTasks.value) {
    for(const task of projectBackendTasks.value) {
      if(task.status == TaskStatus.Running) {
        return task;
      }
    }
  }
  return new BackendTaskPayload()
})

// Simulate fetching new log lines
function fetchLogs(raw : string) {
  return raw.split("\n").map((line) => {
    return {
      id: logId++, // Unique ID
      text: line,
    }
  })
}

// Function to display logs with smooth updates
async function updateDisplayedLogs(newLogs: { id: number; text: string }[]) {
  const delay = (pollingInterval / newLogs.length) * 0.75; // Delay per line

  for (const log of newLogs) {
    await new Promise(resolve => setTimeout(resolve, delay)); // Wait for the delay

    // Add the new log to the displayed logs
    displayedLogs.value.push(log);

    // Ensure the number of displayed logs doesn't exceed the maximum
    if (displayedLogs.value.length > maxDisplayedLogs) {
      displayedLogs.value.shift(); // Remove the oldest log
    }
  }
}

function queryBackend() {
  visible.value = currentTask.value.id >= 0
  if(currentTask.value.id >= 0) {
    api.get<string>(`/task/${currentTask.value.id}/running-log`).then((response) => {
      const newLogs = fetchLogs(response.data + "")
      updateDisplayedLogs(newLogs)
    });
  }
}

useIntervalFn(queryBackend, 2500);

</script>
<style scoped lang="scss">
.log {
  font-family: monospace;
  text-wrap: wrap;
  font-size: 0.65rem;
  text-overflow: ellipsis;
  overflow: hidden;
  height: 150px;
}

.log-container {
  font-family: monospace;
  text-wrap: wrap;
  font-size: 0.65rem;
  height: 150px;
  overflow: hidden;
  position: relative;
}

.log-line {
  white-space: nowrap;
}

.log-enter-active, .log-leave-active {
  transition: transform 0.5s linear, opacity 0.5s ease;
}

.log-enter-from {
  //transform: translateY(100%);
  opacity: 0.5;
}

.log-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}
</style>