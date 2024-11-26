<template>
  <q-btn color="cyan" @click="goToResultsView">
    <q-icon name="archive" class="q-mr-md" />
    <div>Results</div>
    <q-badge floating class="container-badge">
      <q-badge color="green" v-if="numNew > 0"
        >{{ numNew }}
      </q-badge>
    </q-badge>
  </q-btn>
</template>
<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { ResultPayload } from 'src/types/results';

const $router = useRouter();
const numNew = computed(() => {
  if(resultsList.value) {
    return resultsList.value.filter(result => !result.viewed).length;
  }
  return 0;
});

const props = defineProps<{
  projectId?: string;
}>();
const resultsList = defineModel<ResultPayload[]>()


// const lastTaskStates: Record<string, TaskStatus> = {};
// let lastTaskStatesInitialized = false;
//
function goToResultsView() {
  $router.push(`/results/list/${props.projectId}`);
}
//
// function handleNewTaskDetected(task: BackendTaskPayload) {
//   sendInfoNotification(
//     `Started "${task.name}" on ${task.imageIds.length} images`
//   );
// }
//
// function handleTaskStatusChanged(
//   task: BackendTaskPayload,
//   from: TaskStatus,
//   to: TaskStatus
// ) {
//   if (from == TaskStatus.Running && to == TaskStatus.Failed) {
//     sendFailureNotification(`The task "${task.name}" has failed`);
//   } else if (from == TaskStatus.Running && to == TaskStatus.Successful) {
//     sendSuccessNotification(`The task "${task.name}" was successful`);
//   }
// }

// watch(projectBackendTasks, () => {
//   if (projectBackendTasks.value) {
//     if (!lastTaskStatesInitialized) {
//       // Do a pure initialization
//       for (let task of projectBackendTasks.value) {
//         lastTaskStates[task.id.toString()] = task.status;
//       }
//       lastTaskStatesInitialized = true;
//     } else {
//       // We do an update and watch for changes
//       for (let task of projectBackendTasks.value) {
//         if (!lastTaskStates[task.id.toString()]) {
//           handleNewTaskDetected(task);
//         } else if (lastTaskStates[task.id.toString()] != task.status) {
//           handleTaskStatusChanged(
//             task,
//             lastTaskStates[task.id.toString()],
//             task.status
//           );
//         }
//         lastTaskStates[task.id.toString()] = task.status;
//       }
//     }
//   }
// });
</script>
<style scoped lang="scss">
.container-badge {
  background: transparent;
  top: -10px;
  right: -10px;
  gap: 2px;
}
</style>
