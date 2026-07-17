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
  <q-btn to="/" size="lg" flat>
    <img :src="logo" class="logo" alt="Logo"/>
<!--    <q-icon left size="3em" name="fa-solid fa-chart-simple" />-->
<!--    <div>J-AST</div>-->
  </q-btn>
</template>
<script setup lang="ts">
import logo from "assets/logo-j-ast-full-dark.svg"
</script>
<style scoped lang="scss">
.logo {
  height: 3em;
}
</style>
