<script setup lang="ts">
import {ref} from "vue";

const displayed = defineModel<boolean>()
const localProjectName = ref("")

defineProps({
  projectName: String
});
const emit = defineEmits(["onProjectNameChanged"])

function saveProjectName() {
  displayed.value = false;
  emit("onProjectNameChanged", localProjectName.value);
}

</script>

<template>
  <q-dialog v-model="displayed" persistent>
    <q-card style="min-width: 350px">
      <q-card-section>
        <div class="text-h6">Your address</div>
      </q-card-section>

      <q-card-section class="q-pt-none">
        <q-input dense v-model="localProjectName" autofocus @keyup.enter="saveProjectName"/>
      </q-card-section>

      <q-card-actions align="right" class="text-primary">
        <q-btn flat label="Cancel" v-close-popup/>
        <q-btn flat label="Edit" v-close-popup @click="saveProjectName"/>
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<style scoped>

</style>
