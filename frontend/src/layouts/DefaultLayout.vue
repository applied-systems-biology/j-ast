<template>
  <q-layout view="lHh Lpr lFf">
    <q-header>
      <q-toolbar>
        <q-toolbar-title>
          <HeaderLogoButtonComponent/>
        </q-toolbar-title>
        <LoginButtonComponent/>
      </q-toolbar>
    </q-header>
    <q-page-container>
      <router-view />
    </q-page-container>
    <q-footer>
      <ImprintComponent />
    </q-footer>
  </q-layout>
</template>

<script setup lang="ts">
import ImprintComponent from "components/layout/ImprintComponent.vue";
import LoginButtonComponent from "components/layout/AuthManagerComponent.vue";
import HeaderLogoButtonComponent from "components/layout/HeaderLogoButtonComponent.vue";

defineOptions({
  name: 'DefaultLayout'
});
</script>
