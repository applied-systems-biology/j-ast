<template>
  <konva-stage
    ref="stage"
    :config="stageConfig"
    @mousedown="onStageMouseDown"
    @mousemove="onStageMouseMove"
    @mouseup="onStageMouseUp"
    @mouseenter="onStageMouseEnter"
    @mouseleave="onStageMouseLeave">
    <konva-layer>
      <konva-image :config="backgroundImageConfig"/>
    </konva-layer>
    <konva-layer ref="foregroundLayer" :config="{ opacity: 0.5 }">
      <konva-image :config="foregroundImageConfig"/>
    </konva-layer>
    <konva-layer>
      <konva-circle :config="brushPreviewConfig"
      />
    </konva-layer>
  </konva-stage>
</template>
<script setup lang="ts">
import {onMounted, reactive, ref, useTemplateRef, watch} from 'vue';
import {api} from "boot/axios";
import {useQuasar} from "quasar";
import {ImageAnnotationPayload, loadImageElementFromDataString} from "src/types/common";
import {onBeforeRouteLeave} from "vue-router";
import {useEventListener} from "@vueuse/core";

const $q = useQuasar()
const imageAnnotation = defineModel<ImageAnnotationPayload>()

const stageConfig = reactive({
  width: 16,
  height: 16,
  draggable: false
});

const brushPreviewConfig = reactive({
  x: 200,
  y: 100,
  radius: 50,
  stroke: 'cyan',
  dash: [1, 2],
  visible: false
})

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
let isEdited = false
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

  isEdited = true
}

function updatePreview() {
  if(!stage.value || !foregroundLayer.value) {
    return
  }

  const pos = stage.value.getStage().getPointerPosition()
  brushPreviewConfig.x = pos.x
  brushPreviewConfig.y = pos.y
  brushPreviewConfig.visible = true
}

function onStageMouseEnter() {
  brushPreviewConfig.visible = true
}

function onStageMouseLeave() {
  brushPreviewConfig.visible = false
}

function onStageMouseDown() {
  isMouseDown = true
}

function onStageMouseMove(event: MouseEvent) {
  updatePreview()
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

function clear() {

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
        isEdited = false

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
  saveImage,
  queryFromBackend,
  clear
})
// When the user leave the page in your Vue app
onBeforeRouteLeave(() => {
  if (isEdited && !confirm("You have unsaved changes. Are you sure you want to leave?")) {
    return false;
  }
});

// When the user refresh/leave the current tab
useEventListener(window, "beforeunload", (event) => {
  if (isEdited) {
    event.preventDefault();
  }
});

</script>
<style lang="scss">
.konvajs-content {
  border: 1px solid black;
  cursor: crosshair;
}
</style>

