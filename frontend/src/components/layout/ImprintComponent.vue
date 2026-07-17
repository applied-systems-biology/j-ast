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
  <div class="flex text-caption q-pa-md flex-center">
    <div class="col-auto">
      J-AST was developed by Research Group Applied Systems Biology<br/>
      Head: Prof. Dr. Marc Thilo Figge HKI-Center for Systems Biology of
      Infection<br/>
      Leibniz Institute for Natural Product Research and Infection Biology –
      Hans Knöll Institute (HKI)<br/>
      Adolf-Reichwein-Straße 23, 07745 Jena, Germany
    </div>
    <div class="col flex flex-center">
      <q-btn
          v-if="isWebApp()"
          flat
          label="Imprint"
          no-caps no-wrap
          @click="goToImprint()"
      />
      <q-btn
          v-if="isWebApp()"
          flat
          label="Data protection statement"
          no-caps no-wrap
          @click="goToPrivacyStatement()"
      />
      <q-btn
          v-if="isWebApp() && providerInfo"
          :label="providerInfo.providerName"
          flat
          no-caps no-wrap
          @click="goToProvider()"
      />
    </div>
    <div>
      <q-img :src="logoHKI" class="logo-img" fit="contain"/>
    </div>
    <div>
      <q-img :src="logoNFDI" class="logo-img" fit="contain"/>
    </div>
  </div>
</template>
<script lang="ts" setup>
import logoHKI from 'assets/logo-hki.png';
import logoNFDI from 'assets/logo-nfdi4bioimage.png';
import {isWebApp} from 'src/types/electron';
import {useRouter} from 'vue-router';
import {ProviderInfoPayload} from 'src/types/provider';
import {onMounted, ref} from 'vue';
import {loadPayloadInstanceFromApi} from 'src/types/common';

const $router = useRouter();
const providerInfo = ref<ProviderInfoPayload | null>(null);

function goToImprint(): void {
  $router.push('/imprint');
}

function goToPrivacyStatement(): void {
  $router.push('/privacy');
}

function goToProvider(): void {
  if (providerInfo.value) {
    window.open(providerInfo.value.providerUrl, '_blank');
  }
}

onMounted(() => {
  loadPayloadInstanceFromApi("/provider/info", ProviderInfoPayload, providerInfo)
});
</script>
<style scoped>
.logo-img {
  width: 200px;
  height: 50px;
}
</style>
