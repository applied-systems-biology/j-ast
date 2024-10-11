<template>
  <BNavbar>
    <BNavbarBrand to="/">J-AST</BNavbarBrand>
    <BCollapse id="nav-collapse" is-nav>
      <BNavbarNav>
        <BNavItem to="/">Analyze</BNavItem>
        <BNavItem to="/documentation">Documentation</BNavItem>
        <BNavItem to="/about">About</BNavItem>
      </BNavbarNav>
      <BNavbarNav class="ms-auto mb-2 mb-lg-0">
        <BNavItem>Login</BNavItem>
      </BNavbarNav>
    </BCollapse>
  </BNavbar>
  <router-view/>
</template>
<script setup>
import {BNavbar, BNavbarBrand, BNavItem, BCollapse, BNavbarNav, useColorMode} from "bootstrap-vue-next";
  import {onMounted} from "vue";
  const mode = useColorMode();

  onMounted(function () {
    mode.value = "light";
  })
</script>
