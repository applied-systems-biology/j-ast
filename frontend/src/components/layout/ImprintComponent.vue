<template>
  <div class="flex text-caption q-pa-md flex-center">
    <div class="col-auto">
      J-AST was developed by Research Group Applied Systems Biology<br />
      Head: Prof. Dr. Marc Thilo Figge HKI-Center for Systems Biology of
      Infection<br />
      Leibniz Institute for Natural Product Research and Infection Biology –
      Hans Knöll Institute (HKI)<br />
      Adolf-Reichwein-Straße 23, 07745 Jena, Germany
    </div>
    <div class="col flex flex-center">
      <q-btn
        v-if="isWebApp()"
        label="Imprint"
        flat
        no-caps
        @click="goToImprint()"
      />
      <q-btn
        v-if="isWebApp()"
        label="Data protection statement"
        flat
        no-caps
        @click="goToPrivacyStatement()"
      />
      <q-btn
        v-if="isWebApp() && providerInfo"
        :label="providerInfo.providerName"
        flat
        no-caps
        @click="goToProvider()"
      />
    </div>
    <div>
      <q-img class="logo-img" :src="logoHKI" fit="contain" />
    </div>
    <div>
      <q-img class="logo-img" :src="logoNFDI" fit="contain" />
    </div>
  </div>
</template>
<script setup lang="ts">
import logoHKI from 'assets/logo-hki.png';
import logoNFDI from 'assets/logo-nfdi4bioimage.png';
import { isWebApp } from 'src/types/electron';
import { useRouter } from 'vue-router';
import { ProviderInfoPayload } from 'src/types/provider';
import { onMounted, ref } from 'vue';
import { loadPayloadInstanceFromApi } from 'src/types/common';

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
