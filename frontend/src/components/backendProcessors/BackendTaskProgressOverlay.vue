<template>
  <q-dialog v-model="visible" seamless position="bottom">
    <q-card :style="{ width: '500px', maxWidth: '70vw' }">
      <q-card-section class="row no-wrap">
        <div class="col-grow text-bold" >
          Current task progress
        </div>
        <ToggleButton size="sm" flat selected-icon="fa-solid fa-angles-up" not-selected-icon="fa-solid fa-angles-down" v-model="collapsed"/>
      </q-card-section>
      <q-card-section class="log" v-if="!collapsed">
       {{ logText }}
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

const visible = ref(false)
const collapsed = ref(false);
const logText = ref("")
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

function queryBackend() {
  visible.value = currentTask.value.id >= 0
  if(currentTask.value.id >= 0) {
    api.get<string>(`/task/${currentTask.value.id}/running-log`).then((response) => {
      logText.value = response.data + ""
    });
  }
  // visible.value = true
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
</style>