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
  <q-layout view="lHh Lpr lFf">
    <q-header>
      <q-toolbar>
        <q-toolbar-title>
          <HeaderLogoButtonComponent/>
        </q-toolbar-title>
        <UserManagerComponent/>
        <DocumentationComponent/>
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
import UserManagerComponent from "components/layout/UserManagerComponent.vue";
import HeaderLogoButtonComponent from "components/layout/HeaderLogoButtonComponent.vue";
import DocumentationComponent from "components/layout/DocumentationComponent.vue";

defineOptions({
  name: 'DefaultLayout'
});
</script>
