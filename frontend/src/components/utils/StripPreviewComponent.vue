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
  <div class="ticks-preview" :style="{ width: (effectiveTicks.length) * 48 + 'px' }">
    <template v-for="(mark, index) in effectiveTicks" :key="index">
      <div class="tick">
        <div>|</div>
        <div>{{ mark }}</div>
      </div>
    </template>
  </div>
</template>
<script setup lang="ts">
import {computed} from 'vue';

const props = defineProps<{
  ticks: Array<number> | undefined;
}>();

const effectiveTicks = computed(() => props.ticks ?? []);


</script>
<style scoped lang="scss">
.ticks-preview {
  display: flex;
  flex-direction: row;
  background-color: aliceblue;
  border-radius: 5px;
  border: 1px solid gray;
}

.ticks-preview .tick {
  width: 48px;
  text-align: center;
}
</style>
