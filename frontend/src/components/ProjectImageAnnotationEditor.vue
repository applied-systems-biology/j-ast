<template>
  aa
  <konva-stage ref="stage" :config="stageConfig">
    <konva-layer>
      <!-- Rectangle Example -->
      <konva-rect
        :config="rectConfig"
      />
    </konva-layer>

<!--      &lt;!&ndash; Freehand Drawing Layer &ndash;&gt;-->
<!--      <konva-layer ref="drawingLayer">-->
<!--        <konva-line-->
<!--          konva-for="(line, index) in lines"-->
<!--          :key="index"-->
<!--          :config="{-->
<!--              points: line.points,-->
<!--              stroke: 'red',-->
<!--              strokeWidth: 3,-->
<!--              lineCap: 'round',-->
<!--              lineJoin: 'round'-->
<!--            }"-->
<!--        />-->
<!--      </konva-layer>-->
  </konva-stage>
<!--  <q-btn @click="clearDrawing" label="Clear Drawing" color="primary" />-->
</template>
<script setup lang="ts">
// import { ref, reactive } from 'vue';
// import Stage from "vue-konva"
// import Layer from 'vue-konva';
// import Rect from "vue-konva"
import { reactive } from 'vue';
// import { Stage, Layer, Rect, Line } from 'vue-konva';

const stageConfig = reactive({
  width: 800,
  height: 600,
  draggable: false
});

const rectConfig = reactive({
  x: 50,
  y: 50,
  width: 200,
  height: 100,
  fill: 'blue',
  draggable: true
});
//
// const lines = ref([]);
// let isDrawing = false;
//
// const handleClick = () => {
//   console.log('Rectangle clicked');
// };
//
// const startDrawing = (event) => {
//   isDrawing = true;
//   lines.value.push({ points: [] });
// };
//
// const draw = (event) => {
//   if (!isDrawing) return;
//   const stage = event.target.getStage();
//   const pointer = stage.getPointerPosition();
//   const line = lines.value[lines.value.length - 1];
//   line.points.push(pointer.x, pointer.y);
// };
//
// const endDrawing = () => {
//   isDrawing = false;
// };
//
// const clearDrawing = () => {
//   lines.value = [];
// };
</script>
<style scoped lang="scss">
#container {
  width: 100%;
  height: 100%;
}
</style>
