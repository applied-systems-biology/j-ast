<template>
  <konva-stage ref="stage" :config="stageConfig">
    <konva-layer>
      <konva-image :config="backgroundImageConfig" />
    </konva-layer>
    <konva-layer ref="maskLayer">
      <konva-rect
        :config="rectConfig"
      />
    </konva-layer>
  </konva-stage>
  <q-btn @click="saveImage">Save</q-btn>
<!--  <q-btn @click="clearDrawing" label="Clear Drawing" color="primary" />-->
</template>
<script setup lang="ts">
import {onMounted, reactive, useTemplateRef} from 'vue';
import {api} from "boot/axios";

type KonvaImageConfig = {
  image: HTMLImageElement | null;
}

const stageConfig = reactive({
  width: 16,
  height: 16,
  draggable: false
});

const backgroundImageConfig : KonvaImageConfig = reactive({
  image: null
})

const rectConfig = reactive({
  x: 50,
  y: 50,
  width: 200,
  height: 100,
  fill: 'blue',
  draggable: true
});

const stage = useTemplateRef<any>("stage")
const maskLayer = useTemplateRef<any>("maskLayer")

function saveImage() {
  if(maskLayer.value) {
    console.log(maskLayer.value.getNode().toDataURL())
    // console.log(stage.value.toDataURL())
  }
}

onMounted(() => {
  api.get(`/image/52/raw`, { responseType: 'blob' }).then(response => {
    const imageUrl = URL.createObjectURL(response.data); // Create a URL from the blob

    // Create a new Image object
    const imageObj = new Image();
    imageObj.src = imageUrl;

    // Wait for the image to load
    imageObj.onload = () => {
      // Update the image config once the image is loaded
      backgroundImageConfig.image = imageObj;
      stageConfig.width = imageObj.width;
      stageConfig.height = imageObj.height;
    };
  })
})

</script>
<style lang="scss">
.konvajs-content {
  border: 1px solid black;
}
</style>
