<template>
  <q-card>
    <q-card-section class="flex" horizontal>
      <q-card-section class="col-grow">
        <div class="text-h6">Metadata</div>
        <div class="text-blue">Strip profile</div>
        <div v-if="annotation.version > 0" class="text-caption">
          Version {{ annotation.version }}
        </div>
        <div v-else class="text-caption text-red">Not set</div>
      </q-card-section>
    </q-card-section>
    <q-separator/>
    <q-card-actions>
      <q-btn :disable="props.disable" flat icon="edit" @click="openEditor">Edit</q-btn>
    </q-card-actions>
  </q-card>
</template>
<script lang="ts" setup>
import {ref} from 'vue';
import {ImagePayload, MaskImageAnnotationPayload} from 'src/types/image';

// const $q = useQuasar();
const image = defineModel<ImagePayload>();
const annotation = ref<MaskImageAnnotationPayload>(
    new MaskImageAnnotationPayload()
);

const props = defineProps<{
  disable: boolean;
}>();

function openEditor() {
  // TODO
  image
  // // router.push(`/mask-image-annotation/${image.value?.id}/${props.annotationTypeId}`)
  // Dialog.create({
  //   component: MaskImageAnnotationEditorDialog,
  //   componentProps: {
  //     imageId: image.value?.id,
  //     annotationTypeId: props.annotationTypeId,
  //     persistent: true,
  //   },
  // })
  //   .onOk(() => {
  //     if(image.value) {
  //       image.value.version++
  //       incrementImageMaskAnnotationVersion(image.value, props.annotationTypeId);
  //       queryBackend()
  //     }
  //   })
  //   .onCancel(() => {})
  //   .onDismiss(() => {});
}
</script>
<style lang="scss" scoped>
.thumbnail {
  width: 100px;
  height: 100px;
  margin: 4px;
}
</style>
