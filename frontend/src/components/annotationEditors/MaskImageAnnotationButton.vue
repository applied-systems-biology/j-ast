<template>
  <q-card>
    <q-card-section horizontal class="flex">
      <q-card-section class="col-grow">
        <div class="text-h6">Annotation</div>
        <div class="text-blue">{{ annotationTypeName }}</div>
        <div v-if="annotation.version > 0" class="text-caption">
          Version {{ annotation.version }}
        </div>
        <div v-else class="text-caption text-red">Not set</div>
      </q-card-section>
      <q-skeleton class="thumbnail" type="rect" v-if="!thumbnailData" />
      <q-img class="thumbnail" :src="thumbnailData" v-if="thumbnailData" />
    </q-card-section>
    <q-separator />
    <q-card-actions>
      <q-btn :disable="props.disable" flat icon="edit" @click="openEditor" label="Edit" />
      <q-space />
      <q-btn :disable="props.disable" flat icon="download" @click="downloadMask" />
    </q-card-actions>
  </q-card>
</template>
<script setup lang="ts">
import {
  downloadFromApi,
  loadDataStringFromApi,
  loadPayloadInstanceFromApi,
  removeExtensionIfPresent,
  renderMaskAnnotationId,
} from 'src/types/common';
import { computed, onMounted, ref, watch } from 'vue';
import { Dialog, useQuasar } from 'quasar';
import { useMaskImageAnnotationThumbnailStore } from 'stores/mask-image-annotation-thumbnail-store';
import {ImagePayload, incrementImageMaskAnnotationVersion, MaskImageAnnotationPayload} from 'src/types/image';
import MaskImageAnnotationEditorDialog from './MaskImageAnnotationEditorDialog.vue';

const $q = useQuasar();
// const router = useRouter()
const image = defineModel<ImagePayload>({ required: true });
const annotation = ref<MaskImageAnnotationPayload>(
  new MaskImageAnnotationPayload()
);
const thumbnailData = ref<string>();
const annotationTypeName = computed(() =>
  image.value
    ? renderMaskAnnotationId(image.value!, props.annotationTypeId)
    : props.annotationTypeId
);
const thumbnailStore = useMaskImageAnnotationThumbnailStore();

const props = defineProps<{
  annotationTypeId: string;
  disable: boolean;
}>();

function openEditor() {
  // router.push(`/mask-image-annotation/${image.value?.id}/${props.annotationTypeId}`)
  Dialog.create({
    component: MaskImageAnnotationEditorDialog,
    componentProps: {
      imageId: image.value?.id,
      annotationTypeId: props.annotationTypeId,
      persistent: true,
    },
  })
    .onOk(() => {
      if(image.value) {
        image.value.version++
        incrementImageMaskAnnotationVersion(image.value, props.annotationTypeId);
        queryBackend()
      }
    })
    .onCancel(() => {})
    .onDismiss(() => {});
}

function downloadMask() {
  if (image.value) {
    $q.loading.show({ message: 'Preparing download ...' });
    downloadFromApi(
      `/mask-image-annotation/${image.value?.id}/${props.annotationTypeId}/raw`,
      removeExtensionIfPresent(image.value?.fileName) +
        '_' +
        props.annotationTypeId +
        '.png'
    ).finally(() => {
      $q.loading.hide();
    });
  }
}

function queryBackend() {
  if (image.value && image.value.id >= 0) {
    loadDataStringFromApi(
      `/mask-image-annotation/${image.value?.id}/${props.annotationTypeId}/thumbnail`
    ).then((data) => {
      thumbnailData.value = data;
    });
    loadPayloadInstanceFromApi(
      `/mask-image-annotation/${image.value?.id}/${props.annotationTypeId}`,
      MaskImageAnnotationPayload,
      annotation
    ).then((payload) => {
      thumbnailStore
        .fetchImage(payload.imageId, payload.annotationTypeId, payload.version)
        .then((dataUrl) => {
          thumbnailData.value = dataUrl;
        });
    });
  }
}

onMounted(() => {
  queryBackend();
});
watch(image, queryBackend);
</script>
<style scoped lang="scss">
.thumbnail {
  width: 100px;
  height: 100px;
  margin: 4px;
}
</style>
