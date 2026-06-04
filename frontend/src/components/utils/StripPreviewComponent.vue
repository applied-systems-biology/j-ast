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