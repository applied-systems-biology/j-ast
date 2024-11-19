<template>
  <q-dialog ref="dialogRef" @hide="onDialogHide">
    <q-card class="q-dialog-plugin">
      <q-card-section>
        <div class="text-h6">Auto-sort by metadata</div>
      </q-card-section>
      <q-separator />
      <q-card-section>
        <div class="text-bold">
          <q-icon name="help" />
          Info
        </div>
        <div>
          Images with the same experiment, sample, and assay type will be sorted into the same row.
          The column is determined by the time point order.
          Images that are already sorted into the table will be not affected by this operation.
        </div>
      </q-card-section>
      <q-separator />
      <q-card-section class="q-gutter-sm">
        <q-select filled v-model="payload.timePointOrder" multiple label="Time point order" :options="timePointOptions" clearable >
          <template v-slot:selected-item="item">
            <q-chip
              removable
              dense
              @remove="item.removeAtIndex(item.index)"
              :tabindex="item.tabindex"
              color="amber-9"
              text-color="white"
              class="badge"
            >
              <q-icon name="fa-solid fa-clock" class="q-mr-sm"/>
              <div>{{ item.opt }}</div>
            </q-chip>
          </template>
        </q-select>
        <div v-if="payload.timePointOrder.length == 0" class="text-caption text-red">Please select at least one time point!</div>
      </q-card-section>
      <q-card-actions align="right">
        <q-btn color="blue-grey" label="Cancel" @click="onDialogCancel" />
        <q-btn color="red" label="OK" @click="onOKClick" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>
<script lang="ts">

export interface ImageAutoSortByMetadataDialogPayload {
  timePointOrder: Array<string>;
}
</script>
<script setup lang="ts">
import { useDialogPluginComponent } from 'quasar';
import {computed, onMounted, ref} from 'vue';
import {ImagePayload, ProjectImagesPayload} from "src/types/common";

const payload = ref<ImageAutoSortByMetadataDialogPayload>({
  timePointOrder: []
});

const props = defineProps<{
  images: ImagePayload[];
  projectImages: ProjectImagesPayload;
}>()

defineEmits([
  // REQUIRED; need to specify some events that your
  // component will emit through useDialogPluginComponent()
  ...useDialogPluginComponent.emits,
]);

const initialTimePointOrder = computed(() => {
  const maxColumn = props.projectImages.maxColumn()
  const result : Array<string> = []
  for (let i = 0; i <= maxColumn ; i++) {
    const badges = props.projectImages.getColumnMetadataAsBadges(i)
    if(badges.length == 1) {
      if(!result.includes(badges[0].text)) {
        result.push(badges[0].text)
      }
    }
  }
  return result
})

const timePointOptions = computed(() => {
  const result = new Set<string>()
  for(const id of props.projectImages.imageIds) {
    if(props.projectImages.imagesById[id].timePoint) {
      result.add(props.projectImages.imagesById[id].timePoint);
    }
  }
  return [...result]
})

const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } =
  useDialogPluginComponent();
// dialogRef      - Vue ref to be applied to QDialog
// onDialogHide   - Function to be used as handler for @hide on QDialog
// onDialogOK     - Function to call to settle dialog with "ok" outcome
//                    example: onDialogOK() - no payload
//                    example: onDialogOK({ /*...*/ }) - with payload
// onDialogCancel - Function to call to settle dialog with "cancel" outcome

// this is part of our example (so not required)
function onOKClick() {
  // on OK, it is REQUIRED to
  // call onDialogOK (with optional payload)
  onDialogOK(payload.value);
  // or with payload: onDialogOK({ ... })
  // ...and it will also hide the dialog automatically
}

onMounted(() => {
  payload.value.timePointOrder = initialTimePointOrder.value;
})

</script>
<style scoped lang="scss">
.q-dialog-plugin {
  width: 700px;
  max-width: 80vw;
}

.badge {
  height: 2em;
  border-radius: 5px;
  gap: 5px;
}
</style>
