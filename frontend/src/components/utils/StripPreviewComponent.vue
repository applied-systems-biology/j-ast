<template>
  <div class="ticks-preview" :style="{ width: (props.ticks.length) * 48 + 'px' }">
    <template v-for="(mark, index) in props.ticks" :key="index">
      <div class="tick">
        <div>|</div>
        <div>{{ mark }}</div>
      </div>
    </template>
  </div>
</template>
<script setup lang="ts">
const props = defineProps<{
  ticks: Array<number>;
}>();

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