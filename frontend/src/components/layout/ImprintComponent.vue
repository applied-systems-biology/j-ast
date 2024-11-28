<script setup lang="ts">
import logoHKI from "assets/logo-hki.png"
import logoNFDI from "assets/logo-nfdi4bioimage.png"
</script>

<template>
  <div class="flex text-caption q-pa-md flex-center">
    <div class="col-auto">
      J-AST was developed by Research Group Applied Systems Biology<br/>
      Head: Prof. Dr. Marc Thilo Figge HKI-Center for Systems Biology of Infection<br/>
      Leibniz Institute for Natural Product Research and Infection Biology – Hans Knöll Insitute (HKI)<br/>
      Adolf-Reichwein-Straße 23, 07745 Jena, Germany
    </div>
    <div class="col">

    </div>
    <div>
      <q-img class="logo-img" :src="logoHKI" fit="contain"/>
    </div>
    <div>
      <q-img class="logo-img" :src="logoNFDI" fit="contain"/>
    </div>
  </div>
</template>

<style scoped>
.logo-img {
  width: 200px;
  height: 50px;
}
</style>
