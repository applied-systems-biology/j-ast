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
  <q-btn color="cyan" no-caps no-wrap @click="goToResultsView">
    <q-icon class="q-mr-md" name="archive"/>
    <div>Results</div>
    <q-badge class="container-badge" floating>
      <q-badge v-if="numNew > 0" color="green"
      >{{ numNew }}
      </q-badge>
    </q-badge>
  </q-btn>
</template>
<script lang="ts" setup>
import {computed} from 'vue';
import {useRouter} from 'vue-router';
import {ResultPayload} from 'src/types/results';

const $router = useRouter();
const numNew = computed(() => {
  if (resultsList.value) {
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
<style lang="scss" scoped>
.container-badge {
  background: transparent;
  top: -10px;
  right: -10px;
  gap: 2px;
}
</style>
