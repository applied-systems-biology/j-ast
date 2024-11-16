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
  router.push(`/image-annotation/${model.value?.id}/${props.annotationTypeId}`)
}

</script>
<style scoped>

</style>
