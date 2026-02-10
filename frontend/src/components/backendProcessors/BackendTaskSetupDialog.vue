<template>
  <q-dialog ref="dialogRef" @hide="onDialogHide">
    <q-card class="q-dialog-plugin">
      <q-card-section>
        <div class="text-h6">{{ props.taskType.name }}</div>
      </q-card-section>
      <q-separator/>
      <q-card-section>
        <q-scroll-area class="w-100" style="height: 75vh;" visible>
          <q-card>
            <!-- Info section -->
            <q-card-section>
              <div class="text-bold">
                <q-icon name="help"/>
                Info
              </div>
              <div class="q-mb-md">{{ props.taskType.description }}</div>
              <div v-if="props.taskType.outputsResult" class="text-blue">
                <q-icon name="archive"/>
                This operation will create items in the <i>Results</i> section.
              </div>
              <div>
                Depending on the task and the number of images, this will take a few
                minutes.
              </div>
              <div
                  v-if="props.taskType.workloadMode == BackendTaskWorkloadMode.Single"
              >
                This operation is applied for each image
              </div>
              <div
                  v-if="props.taskType.workloadMode == BackendTaskWorkloadMode.FullRow"
              >
                This operation is applied for the whole row. Your selection was
                updated accordingly.
              </div>
              <div
                  v-if="
            props.taskType.workloadMode == BackendTaskWorkloadMode.FullColumn
          "
              >
                This operation is applied for the whole column. Your selection was
                updated accordingly.
              </div>
              <div
                  v-if="
            validateAllInputs ==
            BackendTaskWorkloadDataSlotValidationResult.MandatoryMissing
          "
                  class="text-red"
              >
                <q-icon name="warning"/>
                There were some issues (e.g., missing inputs, wrong parameters)
                detected. Please review the parameters and the list if processed
                images below.
              </div>
            </q-card-section>
            <!-- Parameters section -->
            <q-separator/>
            <q-card-section v-if="taskType.hasCommonParameters()" class="q-gutter-sm">
              <div class="text-h6">Parameters</div>
              <template
                  v-for="(parameter, index) in payload.parameters"
                  :key="parameter.id"
              >
                <BackendTaskParameterEditor
                    v-if="parameter.type == BackendTaskWorkloadParameterSlotType.Common"
                    v-model="payload.parameters[index]"
                />
              </template>
            </q-card-section>
            <!-- Advanced parameters section -->
            <q-card-section v-if="taskType.hasAdvancedParameters()" class="q-gutter-sm">
              <div class="text-h6">Advanced parameters</div>
              <template
                  v-for="(parameter, index) in payload.parameters"
                  :key="parameter.id"
              >
                <BackendTaskParameterEditor
                    v-if="parameter.type == BackendTaskWorkloadParameterSlotType.Advanced"
                    v-model="payload.parameters[index]"
                />
              </template>
            </q-card-section>
            <!-- Data overview section -->
            <q-separator/>
            <q-card-section>
              <div class="text-h6">The following images will be processed</div>
              <div class="text-bold q-mb-lg">
                <q-icon name="warning"/>
                During the processing, you will not be able to edit the images
              </div>
              <q-table :rows="previewRows">
                <template v-slot:body-cell-image="props">
                  <q-td :props="props">
                    <div>
                      <ProjectImageButton
                          :current-image="props.value"
                          :has-running-task="false"
                          :selected-image-ids="[]"
                          class="image-button"
                      />
                    </div>
                  </q-td>
                </template>
                <template v-slot:body-cell-inputs="props">
                  <q-td :props="props">
                    <template v-for="slot in props.value" :key="slot.slot.name">
                      <div
                          v-if="
                    slot.validation ==
                    BackendTaskWorkloadDataSlotValidationResult.Ok
                  "
                          class="text-green"
                      >
                        <q-icon name="fa-solid fa-check"/>
                        {{ renderDataSlot(slot.slot.name) }}
                      </div>
                      <div
                          v-else-if="
                    slot.validation ==
                    BackendTaskWorkloadDataSlotValidationResult.OptionalMissing
                  "
                          class="text-orange"
                      >
                        <q-icon name="fa-solid fa-circle-info"/>
                        {{ renderDataSlot(slot.slot.name) }}
                      </div>
                      <div v-else class="text-red">
                        <q-icon name="fa-solid fa-xmark"/>
                        {{ renderDataSlot(slot.slot.name) }}
                      </div>
                    </template>
                    <div v-if="props.value.length == 0" class="text-green">
                      <q-icon name="fa-solid fa-check"/>
                      No inputs
                    </div>
                  </q-td>
                </template>
                <template v-slot:body-cell-outputs="props2">
                  <q-td :props="props2">
                    <div v-for="slot in props2.value" :key="slot.slot.name">
                      <q-icon name="fa-solid fa-save"/>
                      {{ renderDataSlot(slot.slot.name) }}
                    </div>
                    <div v-if="props.taskType.outputsResult" class="text-blue">
                      <q-icon name="archive"/>
                      Results
                    </div>
                  </q-td>
                </template>
              </q-table>
            </q-card-section>
          </q-card>
        </q-scroll-area>
      </q-card-section>
      <q-card-actions align="right">
        <q-btn color="blue-grey" label="Cancel" @click="onDialogCancel"/>
        <q-btn color="red" label="OK" @click="onOKClick"/>
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>
<script lang="ts" setup>
import {useDialogPluginComponent} from 'quasar';
import {computed, onMounted, ref} from 'vue';
import {ImagePayload} from 'src/types/image';
import {
  BackendTaskPayload,
  BackendTaskTypePayload,
  BackendTaskWorkloadDataSlot,
  BackendTaskWorkloadDataSlotValidationResult,
  BackendTaskWorkloadMode,
  BackendTaskWorkloadParameterSlotType,
  imageSupportsBackendInputSlot,
  validateImageBackendInputSlot,
} from 'src/types/backendTasks';
import ProjectImageButton from 'components/arranger/ProjectImageButton.vue';
import {renderDataSlot} from 'src/types/common';
import BackendTaskParameterEditor from 'components/backendProcessors/BackendTaskParameterEditor.vue';

const payload = ref<BackendTaskPayload>(new BackendTaskPayload());

const props = defineProps<{
  taskType: BackendTaskTypePayload;
  images: ImagePayload[];
  projectId: number;
}>();

defineEmits([
  // REQUIRED; need to specify some events that your
  // component will emit through useDialogPluginComponent()
  ...useDialogPluginComponent.emits,
]);

const {dialogRef, onDialogHide, onDialogOK, onDialogCancel} =
    useDialogPluginComponent();

function onOKClick() {
  onDialogOK(payload.value);
}

const previewRows = computed(() => {
  const result = [];
  for (const image of props.images) {
    const row: Record<string, any> = {};
    const inputReport: Array<{
      slot: BackendTaskWorkloadDataSlot;
      validation: BackendTaskWorkloadDataSlotValidationResult;
    }> = [];
    const outputReport: Array<{ slot: BackendTaskWorkloadDataSlot }> = [];

    // Check if inputs are present
    for (const slot of props.taskType.inputs) {
      if (imageSupportsBackendInputSlot(image, slot)) {
        inputReport.push({
          slot: slot,
          validation: validateImageBackendInputSlot(image, props.images, slot),
        });
      }
    }

    // Add outputs
    for (const slot of props.taskType.outputs) {
      outputReport.push({
        slot: slot,
      });
    }

    row['image'] = image;
    row['inputs'] = inputReport;
    row['outputs'] = outputReport;
    result.push(row);
  }
  return result;
});

const validateAllInputs = computed(() => {
  let response = BackendTaskWorkloadDataSlotValidationResult.Ok;
  for (const image of props.images) {
    for (const slot of props.taskType.inputs) {
      if (imageSupportsBackendInputSlot(image, slot)) {
        const imageValidation = validateImageBackendInputSlot(
            image,
            props.images,
            slot
        );
        if (
            response == BackendTaskWorkloadDataSlotValidationResult.Ok &&
            imageValidation != BackendTaskWorkloadDataSlotValidationResult.Ok
        ) {
          response = imageValidation;
        } else if (
            response ==
            BackendTaskWorkloadDataSlotValidationResult.OptionalMissing &&
            imageValidation ==
            BackendTaskWorkloadDataSlotValidationResult.MandatoryMissing
        ) {
          response = imageValidation;
        }
      }
    }
  }
  return response;
});

onMounted(() => {
  payload.value.taskId = props.taskType.taskId;
  payload.value.imageIds = props.images.map((image) => image.id);
  payload.value.projectId = props.projectId;

  //Copy over parameters
  for (const parameter of props.taskType.parameters) {
    payload.value.parameters.push(parameter);
  }
});
</script>
<style lang="scss" scoped>
$grid-item-width: 18rem;
$grid-item-height: 8rem;

.q-dialog-plugin {
  width: 1024px;
  max-width: 80vw;
}

.image-button {
  width: $grid-item-width;
  height: $grid-item-height;
}
</style>
