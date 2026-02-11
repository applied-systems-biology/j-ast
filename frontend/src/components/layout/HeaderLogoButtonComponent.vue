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
