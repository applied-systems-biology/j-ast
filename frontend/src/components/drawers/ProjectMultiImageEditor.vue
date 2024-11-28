<template>
<q-card class="q-mb-lg">
<q-card-section>
  {{ imageIds?.length }} selected images
</q-card-section>
</q-card>
</template>
<script setup lang="ts">
const imageIds = defineModel<number[]>()
</script>
<style scoped lang="scss">

</style>
