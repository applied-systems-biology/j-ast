<template>
  <konva-stage
    ref="stage"
    :config="stageConfig"
    @mousedown="onStageMouseDown"
    @mousemove="onStageMouseMove"
    @mouseup="onStageMouseUp">
    <konva-layer>
      <konva-image :config="backgroundImageConfig"/>
    </konva-layer>
    <konva-layer ref="foregroundLayer" :config="{ opacity: 0.5 }">
      <konva-image :config="foregroundImageConfig"/>
    </konva-layer>
  </konva-stage>
</template>
<script setup lang="ts">
import {onMounted, reactive, ref, useTemplateRef, watch} from 'vue';
import {api} from "boot/axios";
import {useQuasar} from "quasar";
import {ImageAnnotationPayload, loadImageElementFromDataString} from "src/types/common";

const $q = useQuasar()
const imageAnnotation = defineModel<ImageAnnotationPayload>()

const stageConfig = reactive({
  width: 16,
  height: 16,
  draggable: false
});

const backgroundImageConfig: { image: HTMLImageElement | null } = reactive({
  image: null
})

const foregroundImageConfig: { image: HTMLCanvasElement | null } = reactive({
  image: null
})

type Position = { x: number; y: number }

const stage = useTemplateRef<any>("stage")
const foregroundLayer = useTemplateRef<any>("foregroundLayer")
const foregroundCanvas = ref<HTMLCanvasElement | null>(null);
const foregroundContext = ref<CanvasRenderingContext2D | null>(null);
let isMouseDown = false;
let lastPosition : Position | null = null;
const maskValueForeground = "#ff0000"
const maskValueBackground = "#00000000"

function drawMask(event: MouseEvent, size: number, maskValue: boolean) {
  if(!stage.value || !foregroundLayer.value) {
    return
  }

  const pos = stage.value.getStage().getPointerPosition()
  const context = foregroundContext.value
  if(!context || !pos) {
    return
  }
  if(!lastPosition) {
    lastPosition = pos as Position
  }

  // console.log("draw")
  // context.save()
  // context.globalCompositeOperation = 'destination-out';
  // context.beginPath();
  // context.arc(pos.x, pos.y, size, 0, Math.PI * 2);
  // context.fill();
  // context.restore();
  //
  // // Update the Konva image with the modified canvas
  // const foregroundImageNode = event.target.getStage().findOne('Image');
  // foregroundImageNode.getLayer().batchDraw();
  // console.log(foregroundImageNode)

  context.strokeStyle = maskValue ? maskValueForeground : maskValueBackground
  context.globalCompositeOperation = "source-over";
  context.lineCap = "round"
  context.lineJoin = "round"
  context.lineWidth = size
  context.beginPath();
  // context.arc(pos.x, pos.y, size, 0, Math.PI * 2);
  context.moveTo(lastPosition.x, lastPosition.y)
  context.lineTo(pos.x, pos.y)
  context.stroke()
  lastPosition = pos as Position
  // context.fill();
  foregroundLayer.value.getNode().batchDraw()
}

function onStageMouseDown() {
  isMouseDown = true
}

function onStageMouseMove(event: MouseEvent) {
  if(isMouseDown) {
    drawMask(event, 15, true)
  }
}

function onStageMouseUp() {
  lastPosition = null
  isMouseDown = false
}

function saveImage() {
  if (foregroundLayer.value) {
    console.log(foregroundLayer.value.getNode().toDataURL())
    // console.log(stage.value.toDataURL())
  }
}

function queryFromBackend() {
  $q.loading.show();
  if (imageAnnotation.value && imageAnnotation.value.imageId >= 0) {

    api.get(`/image/${imageAnnotation.value.imageId}/raw`, {responseType: 'blob'}).then(backgroundResponse => {
      loadImageElementFromDataString(backgroundResponse.data).then((backgroundImage) => {
        stageConfig.width = backgroundImage.width;
        stageConfig.height = backgroundImage.height;
        backgroundImageConfig.image = backgroundImage;

        // Create canvas
        const canvas = document.createElement('canvas');
        canvas.width = stageConfig.width;
        canvas.height = stageConfig.height;
        const context = canvas.getContext('2d')!;
        // context.drawImage(fgImg, 0, 0, stageConfig.width, stageConfig.height);

        foregroundCanvas.value = canvas;
        foregroundContext.value = context;
        foregroundImageConfig.image = canvas;

        $q.loading.hide();
      })
    })
  }
}

onMounted(() => {
  queryFromBackend()
})
watch(imageAnnotation, () => {
  queryFromBackend()
})
defineExpose({
  saveImage
})

</script>
<style lang="scss">
.konvajs-content {
  border: 1px solid black;
}
</style>
<style scoped lang="scss">
.edit-toolbar > * {
  margin-right: 10px;
}
</style>
