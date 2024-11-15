<template>
  <q-btn @click="openEditor">Annotate {{ props.annotationTypeName }}</q-btn>
</template>
<script setup lang="ts">
import { useRouter } from 'vue-router';
import { ImagePayload } from 'src/types/common';

const router = useRouter()
const model = defineModel<ImagePayload>();

const props = defineProps<{
  annotationTypeId: string,
  annotationTypeName: string,
}>()

function openEditor() {
  router.push(`/annotation/${props.annotationTypeId}/${model.value?.id}`)
}

</script>
<style scoped>

</style>
