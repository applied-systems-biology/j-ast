<template>
  <q-card>
    <q-card-section horizontal class="flex">
      <q-card-section class="col-grow">
        <div class="text-h6">Annotation</div>
        <div class="text-blue">{{ annotationTypeName }}</div>
        <div v-if="annotation.version > 0" class="text-caption">Version {{ annotation.version }}</div>
        <div v-else class="text-caption text-red">Not set</div>
      </q-card-section>
      <q-skeleton class="thumbnail" type="rect" v-if="!thumbnailData"/>
      <q-img class="thumbnail" :src="thumbnailData" v-if="thumbnailData"/>
    </q-card-section>
    <q-separator />
    <q-card-actions>
      <q-btn flat icon="edit" @click="openEditor">Edit</q-btn>
      <q-btn flat icon="download" @click="downloadMask">Download</q-btn>
    </q-card-actions>
  </q-card>
</template>
<script setup lang="ts">
import { useRouter } from 'vue-router';
import {
  downloadFromApi,
  ImagePayload,
  loadDataStringFromApi,
  loadPayloadInstanceFromApi,
  MaskImageAnnotationPayload, renderMaskAnnotationId
} from 'src/types/common';
import {computed, onMounted, ref, watch} from "vue";
import {useQuasar} from "quasar";
import { useMaskImageAnnotationThumbnailStore } from 'stores/mask-image-annotation-thumbnail-store';

const $q = useQuasar()
const router = useRouter()
const image = defineModel<ImagePayload>();
const annotation = ref<MaskImageAnnotationPayload>(new MaskImageAnnotationPayload());
const thumbnailData = ref<string>()
const annotationTypeName = computed(() => image.value ? renderMaskAnnotationId(image.value!, props.annotationTypeId) : props.annotationTypeId)
const thumbnailStore = useMaskImageAnnotationThumbnailStore()

const props = defineProps<{
  annotationTypeId: string
}>()

function openEditor() {
  router.push(`/mask-image-annotation/${image.value?.id}/${props.annotationTypeId}`)
}

function downloadMask() {
  $q.loading.show({ message: 'Preparing download ...' });
  downloadFromApi(`/mask-image-annotation/${image.value?.id}/${props.annotationTypeId}/raw`)
    .finally(() => {
      $q.loading.hide();
    })
}

function queryBackend() {
  if(image.value && image.value.id >= 0) {

    loadDataStringFromApi(`/mask-image-annotation/${image.value?.id}/${props.annotationTypeId}/thumbnail`).then((data) => {
      thumbnailData.value = data;
    })
    loadPayloadInstanceFromApi(`/mask-image-annotation/${image.value?.id}/${props.annotationTypeId}`, MaskImageAnnotationPayload, annotation).then(payload => {
      thumbnailStore.fetchImage(payload.imageId, payload.annotationTypeId, payload.version).then(dataUrl => {
        thumbnailData.value = dataUrl;
      })
    })
  }
}

onMounted(() => {
 queryBackend();
})
watch(image, queryBackend)

</script>
<style scoped lang="scss">
.thumbnail {
  width: 100px;
  height: 100px;
  margin: 4px;
}
</style>
