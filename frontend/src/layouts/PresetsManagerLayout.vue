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
          <q-card-section>
            <div class="text-h6">E-Test strips</div>
          </q-card-section>
          <q-separator/>
          <q-card-section class="q-gutter-sm">
            <q-card class="bg-blue-grey-1" v-for="preset in stripPresets" :key="preset.id">
              <q-card-section class="flex">
                <div class="text-h7">{{ preset.name }}</div>
                <q-space />
                <q-icon name="lock"/>
              </q-card-section>
              <q-separator/>
              <q-card-section>
                <q-scroll-area visible class="w-100" style="height: 64px;">
                  <StripPreviewComponent :preset="preset" />
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

defineOptions({
  name: 'PresetsManagerLayout',
});

const stripPresets = ref<StripPresetPayload[]>()

onMounted(() => {
  loadPayloadInstanceFromApi("/get-presets/strip", StripPresetPayload, stripPresets)
})

</script>
