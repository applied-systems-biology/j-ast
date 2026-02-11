<template>
  <q-card>
    <q-card-section class="flex" horizontal>
      <q-card-section class="col-grow">
        <div class="text-h6">Metadata</div>
        <div class="text-blue">Strip preset</div>
        <div v-if="annotation.isPresent()" class="text-caption">
          {{ annotation.getName() }}
        </div>
        <div v-else class="text-caption text-red">Not set</div>
      </q-card-section>
    </q-card-section>
    <q-separator/>
    <q-card-actions>
      <q-btn :disable="props.disable" flat icon="edit" label="Edit" @click="openEditor"/>
    </q-card-actions>
  </q-card>
</template>
<script lang="ts" setup>
import {computed} from 'vue';
import {ImagePayload, setImageMetadata} from 'src/types/image';
import {StripPresetPayload} from "src/types/presets";
import {Dialog} from "quasar";
import StripPresetSelectorDialog from "components/annotationEditors/StripPresetSelectorDialog.vue";
import {sendFailureNotification} from "src/types/notification";
import {plainToInstance} from "class-transformer";

// const $q = useQuasar();
const image = defineModel<ImagePayload>({required: true});
const annotation = computed(() => {
  if(image.value) {
    const v = plainToInstance(ImagePayload, image.value).getMetadata("stripPreset")
    if(v) {
      return plainToInstance(StripPresetPayload, v)
    }
  }
  return new StripPresetPayload()
})

const props = defineProps<{
  disable: boolean;
}>();

function openEditor() {
  // router.push(`/mask-image-annotation/${image.value?.id}/${props.annotationTypeId}`)
  Dialog.create({
    component: StripPresetSelectorDialog,
    componentProps: {
      persistent: true,
    },
  })
    .onOk((payload : StripPresetPayload) => {
      if(image.value && payload) {
        setImageMetadata(image.value, "stripPreset", payload)
        image.value.version += 1
        plainToInstance(ImagePayload, image.value).uploadToBackend().catch(() => {
          sendFailureNotification('Error while updating');
        });
      }
    })
    .onCancel(() => {})
    .onDismiss(() => {});
}
</script>
<style lang="scss" scoped>
.thumbnail {
  width: 100px;
  height: 100px;
  margin: 4px;
}
</style>
