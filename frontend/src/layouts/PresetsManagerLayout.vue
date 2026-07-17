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
  <q-layout view="hHh lpR fFf">
    <q-header>
      <q-toolbar>
        <q-toolbar-title class="row items-center q-gutter-sm">
          <HeaderLogoButtonComponent/>
          <div>/</div>
          <div>Presets</div>
        </q-toolbar-title>
        <UserManagerComponent/>
        <DocumentationComponent/>
      </q-toolbar>
    </q-header>
    <q-page-container>
      <q-page class="q-gutter-sm" padding>
        <q-card>
          <q-card-section class="flex">
            <div class="text-h6">E-Test strips</div>
            <q-space/>
            <q-btn color="green" icon="add" label="Add" no-caps no-wrap @click="createStripPreset"/>
          </q-card-section>
          <q-separator/>
          <q-card-section class="q-gutter-sm">
            <q-card v-for="preset in stripPresets" :key="preset.id" class="bg-blue-grey-1 q-mb-sm">
              <q-card-section class="flex">
                <div class="text-h7">{{ preset.name }}</div>
                <q-space/>
                <div v-if="preset.id > 0" class="q-gutter-sm">
                  <q-btn color="red-4" icon="delete" no-caps @click="deletePreset(preset.id, 'E-Strip preset', preset.name)"/>
                  <q-btn color="primary" icon="edit" label="Edit" no-caps @click="editStripPreset(preset)"/>
                </div>
                <template v-else>
                  <q-icon name="lock"/>
                </template>
              </q-card-section>
              <q-separator/>
              <q-card-section>
                <q-scroll-area class="w-100" style="height: 64px;" visible>
                   <StripPreviewComponent :ticks="preset.getEffectiveTicks()"/>
                </q-scroll-area>
              </q-card-section>
            </q-card>
          </q-card-section>
        </q-card>
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script lang="ts" setup>
import HeaderLogoButtonComponent from 'components/layout/HeaderLogoButtonComponent.vue';
import UserManagerComponent from 'components/layout/UserManagerComponent.vue';
import DocumentationComponent from "components/layout/DocumentationComponent.vue";
import {onMounted, ref} from "vue";
import {StripPresetPayload} from "src/types/presets";
import {loadPayloadInstanceFromApi} from "src/types/common";
import StripPreviewComponent from "components/utils/StripPreviewComponent.vue";
import {Dialog} from "quasar";
import StripPresetEditorDialog from "components/presets/StripPresetEditorDialog.vue";
import {api} from "boot/axios";
import {instanceToPlain} from "class-transformer";
import {sendSuccessNotification} from "src/types/notification";
import {onDialogYes} from "src/types/dialog";

defineOptions({
  name: 'PresetsManagerLayout',
});

const stripPresets = ref<StripPresetPayload[]>()

function createStripPreset() {
  Dialog.create({
    component: StripPresetEditorDialog,
    componentProps: {
      persistent: true,
    },
  })
      .onOk((payload: StripPresetPayload) => {
        api.post("/add-preset/strip", instanceToPlain(payload)).then(() => {
          queryBackend()
          sendSuccessNotification("Created new E-strip preset '" + payload.name + "'")
        })
      })
      .onCancel(() => {
      })
      .onDismiss(() => {
      });
}

function editStripPreset(preset: StripPresetPayload) {
  Dialog.create({
    component: StripPresetEditorDialog,
    componentProps: {
      preset: preset,
      persistent: true,
    },
  })
      .onOk((payload: StripPresetPayload) => {
        api.post("/update-preset/strip", instanceToPlain(payload)).then(() => {
          queryBackend()
          sendSuccessNotification(`Updated E-strip preset '${payload.name}'`)
        })
      })
      .onCancel(() => {
      })
      .onDismiss(() => {
      });
}

function deletePreset(id: number, typeName: string, name: string) {
    onDialogYes("Delete preset", `Do you really want to delete the ${typeName} '${name}'?`).then(() => {
      api.post("/delete-preset/" + id).then(() => {
        queryBackend()
        sendSuccessNotification(`Deleted ${typeName} '${name}'`)
      })
    })
}

function queryBackend() {
  loadPayloadInstanceFromApi("/get-presets/strip", StripPresetPayload, stripPresets)
}

onMounted(() => {
  queryBackend()
})

</script>
