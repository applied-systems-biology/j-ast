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
import ImprintComponent from "components/ImprintComponent.vue";
import LoginButtonComponent from "components/LoginButtonComponent.vue";
import HeaderLogoButtonComponent from "components/HeaderLogoButtonComponent.vue";

defineOptions({
  name: 'IndexLayout'
});
</script>
