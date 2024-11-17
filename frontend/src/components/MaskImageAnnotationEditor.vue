<template>
  <q-card class="q-mb-lg">
    <q-card-section class="row q-gutter-md">
      <div class="col-2">
        <q-badge color="primary">
          Zoom
        </q-badge>
        <q-slider :min="1" :step="1" :max="100" :model-value="100" snap label></q-slider>
      </div>
      <div class="col-2" v-if="currentToolId=='draw' || currentToolId == 'line'">
        <q-badge color="secondary">
          Brush size
        </q-badge>
        <q-slider v-model="brushSize" :min="1" :step="1" :max="100" snap label></q-slider>
      </div>
    </q-card-section>
  </q-card>

  <konva-stage
    ref="stage"
    :config="stageConfig"
    @mousedown="onStageMouseDown"
    @mousemove="onStageMouseMove"
    @mouseup="onStageMouseUp"
    @mouseenter="onStageMouseEnter"
    @mouseleave="onStageMouseLeave"
    @click="onStageMouseClick($event, 1)"
    @dblclick="onStageMouseClick($event, 2)">
    <konva-layer>
      <konva-image :config="backgroundImageConfig"/>
    </konva-layer>
    <konva-layer ref="foregroundLayer" :config="{ opacity: 0.5 }">
      <konva-image :config="foregroundImageConfig"/>
    </konva-layer>
    <konva-layer>
      <konva-circle :config="brushPreviewConfig"/>
      <konva-line :config="linePreviewConfig"/>
    </konva-layer>
  </konva-stage>
</template>
<script setup lang="ts">
import {onMounted, reactive, Ref, ref, useTemplateRef, watch} from 'vue';
import {api} from "boot/axios";
import {useQuasar} from "quasar";
import {MaskImageAnnotationPayload, loadImageElementFromDataString} from "src/types/common";
import {onBeforeRouteLeave} from "vue-router";
import {useEventListener} from "@vueuse/core";
import FloodFill from "q-floodfill";

const $q = useQuasar()
const imageAnnotation = defineModel<MaskImageAnnotationPayload>()
const currentToolColor: Ref<string | undefined> = defineModel<string>("tool-color")
const currentToolId: Ref<string | undefined> = defineModel<string>("tool-id")
const brushSize = ref(20)
const previewHighlighter = "#00ffffaa"

const stageConfig = reactive({
  width: 16,
  height: 16,
  draggable: false
});

const brushPreviewConfig = reactive({
  x: 0,
  y: 0,
  radius: 1,
  stroke: 'cyan',
  visible: false
})

const linePreviewConfig = reactive({
  x: 0,
  y: 0,
  strokeWidth: 1,
  lineCap: 'round',
  lineJoin: 'round',
  points: [0, 0, 100, 100],
  stroke: previewHighlighter,
  visible: false
})

const backgroundImageConfig: { image: HTMLImageElement | null } = reactive({
  image: null
})

const foregroundImageConfig: { image: HTMLCanvasElement | null } = reactive({
  image: null
})

type Position = { x: number; y: number }

enum MouseEventType {
  MouseDown,
  MouseUp,
  MouseClick,
  MouseMove,
  MouseEnter,
  MouseLeave,
  MouseDoubleClick
}

const stage = useTemplateRef<any>("stage")
const foregroundLayer = useTemplateRef<any>("foregroundLayer")
const foregroundCanvas = ref<HTMLCanvasElement | null>(null);
const maskDataContext = ref<CanvasRenderingContext2D | null>(null);
const foregroundContext = ref<CanvasRenderingContext2D | null>(null);
let isEdited = false
let isMouseDown = false;
let lastPosition: Position | null = null;


function doToolDraw() {

  const context = maskDataContext.value

  if (!stage.value || !foregroundLayer.value || !context) {
    return
  }

  const pos = stage.value.getStage().getPointerPosition()

  if (!pos) {
    return
  }
  if (!lastPosition) {
    lastPosition = pos as Position
  }

  context.imageSmoothingEnabled = false
  context.strokeStyle = currentToolColor.value!
  context.globalCompositeOperation = "source-over"
  context.lineCap = "round"
  context.lineJoin = "round"
  context.lineWidth = brushSize.value
  context.beginPath();
  context.moveTo(lastPosition.x, lastPosition.y)
  context.lineTo(pos.x, pos.y)
  context.stroke()
  lastPosition = pos as Position

  renderMaskToForeground()
  isEdited = true
}

function doToolLine(eventType: MouseEventType) {
  if (!stage.value || !foregroundLayer.value) {
    return
  }
  const pos = stage.value.getStage().getPointerPosition()
  if (!pos) {
    return;
  }
  if (eventType == MouseEventType.MouseDown) {
    lastPosition = pos as Position
    linePreviewConfig.strokeWidth = brushSize.value
  } else if (eventType == MouseEventType.MouseMove) {
    if (isMouseDown && lastPosition) {
      linePreviewConfig.visible = true
      linePreviewConfig.points = [lastPosition.x, lastPosition.y, pos.x, pos.y]
    }
  } else if (eventType == MouseEventType.MouseUp) {
    linePreviewConfig.visible = false
    if (lastPosition) {
      const context = maskDataContext.value
      if (!context) {
        return;
      }

      context.imageSmoothingEnabled = false
      context.strokeStyle = currentToolColor.value!
      context.globalCompositeOperation = "source-over"
      context.lineCap = "round"
      context.lineJoin = "round"
      context.lineWidth = brushSize.value
      context.beginPath();
      context.moveTo(lastPosition.x, lastPosition.y)
      context.lineTo(pos.x, pos.y)
      context.stroke()

      renderMaskToForeground()
      isEdited = true
    }
  }
}

function doToolFill() {
  if (!stage.value || !foregroundLayer.value) {
    return
  }
  const pos = stage.value.getStage().getPointerPosition()
  if (!pos) {
    return;
  }
  const context = maskDataContext.value
  if (!context) {
    return;
  }

  // Needed to handle antialiasing
  doThresholding()

  const canvas = context.canvas;
  const imageData = context.getImageData(0, 0, canvas.width, canvas.height);

  const floodFill = new FloodFill(imageData)
  floodFill.fill( currentToolColor.value!, Math.floor(pos.x), Math.floor(pos.y), 0)
  context.putImageData(floodFill.imageData, 0, 0)

  renderMaskToForeground()
  isEdited = true
}

function updatePreview() {
  if (!stage.value || !foregroundLayer.value) {
    return
  }

  const pos = stage.value.getStage().getPointerPosition()
  if (!pos) {
    brushPreviewConfig.visible = false
  }
  if (currentToolId.value === "draw" || currentToolId.value === "line") {
    brushPreviewConfig.x = pos.x
    brushPreviewConfig.y = pos.y
    brushPreviewConfig.visible = true
    brushPreviewConfig.radius = brushSize.value / 2
  } else {
    brushPreviewConfig.visible = false
  }
}

function doTool(eventType: MouseEventType) {
  switch (currentToolId.value) {
    case "draw": {
      if (eventType == MouseEventType.MouseDown) {
        doToolDraw()
      } else if (eventType == MouseEventType.MouseMove) {
        if (isMouseDown) {
          doToolDraw()
        }
      }
    }
      break
    case "line": {
      doToolLine(eventType)
    }
    case "fill": {
      if (eventType == MouseEventType.MouseClick) {
        doToolFill()
      }
    }
  }
}

/**
 * Applies internal thresholding on the mask data, which may be needed for some operations
 * Also done before uploading
 * No need to render afterwards, as the renderer uses the same algorithm
 */
function doThresholding() {
  const context = maskDataContext.value
  if(!context) {
    return;
  }
  const imageData = context.getImageData(0,0,context.canvas.width,context.canvas.height)
  const data = imageData.data;

  for (let i = 0; i < data.length; i += 4) {
    const r = data[i];

    if (r <= 0) {
      data[i] = 0; // Red
      data[i + 1] = 0; // Green
      data[i + 2] = 0; // Blue
      data[i + 3] = 255; // Fully opaque
    } else {
      data[i] = 255; // Red
      data[i + 1] = 255; // Green
      data[i + 2] = 255; // Blue
      data[i + 3] = 255; // Fully opaque
    }
  }

  context.putImageData(imageData, 0, 0)
}

function renderMaskToForeground() {
  const srcContext = maskDataContext.value
  const targetContext = foregroundContext.value
  if(!srcContext || !targetContext) {
    return
  }
  const imageData = srcContext.getImageData(0,0,srcContext.canvas.width,srcContext.canvas.height)
  const data = imageData.data;

  for (let i = 0; i < data.length; i += 4) {
    const r = data[i];

    if (r <= 0) {
      // Map black to fully transparent
      data[i + 3] = 0;
    } else {
      // Map white to red
      data[i] = 255; // Red
      data[i + 1] = 0; // Green
      data[i + 2] = 0; // Blue
      data[i + 3] = 255; // Fully opaque
    }
  }

  targetContext.putImageData(imageData, 0, 0)
  foregroundLayer.value.getNode().batchDraw()
}

function onStageMouseEnter() {
  updatePreview()
  doTool(MouseEventType.MouseEnter)
}

function onStageMouseLeave() {
  updatePreview()
  doTool(MouseEventType.MouseLeave)
}

function onStageMouseDown() {
  isMouseDown = true
  doTool(MouseEventType.MouseDown)
}

function onStageMouseMove() {
  updatePreview()
  doTool(MouseEventType.MouseMove)
}

function onStageMouseUp() {
  isMouseDown = false
  doTool(MouseEventType.MouseUp)
  lastPosition = null
}

function onStageMouseClick(event: MouseEvent, clickCount: number) {
  if (clickCount == 1) {
    doTool(MouseEventType.MouseClick)
  } else if (clickCount == 2) {
    doTool(MouseEventType.MouseDoubleClick)
  }
}

function saveImage() {
  if (foregroundLayer.value) {
    console.log(foregroundLayer.value.getNode().toDataURL())
    // console.log(stage.value.toDataURL())
  }
}

function clear() {
  const context = maskDataContext.value
  if (!stage.value || !foregroundLayer.value || !context) {
    return
  }

  context.imageSmoothingEnabled = false
  context.fillStyle = "black"
  context.globalCompositeOperation = "source-over"
  context.fillRect(0, 0, context.canvas.width, context.canvas.height)
  renderMaskToForeground()

  isEdited = true
}

function queryFromBackend() {
  $q.loading.show();
  if (imageAnnotation.value && imageAnnotation.value.imageId >= 0) {

    api.get(`/image/${imageAnnotation.value.imageId}/raw`, {responseType: 'blob'}).then(backgroundResponse => {
      loadImageElementFromDataString(backgroundResponse.data).then((backgroundImage) => {
        stageConfig.width = backgroundImage.width;
        stageConfig.height = backgroundImage.height;
        backgroundImageConfig.image = backgroundImage;

        // Create a canvas that only holds the mask data
        const dataCanvas = document.createElement('canvas');
        dataCanvas.width = stageConfig.width;
        dataCanvas.height = stageConfig.height;
        const context = dataCanvas.getContext('2d')!;
        // context.drawImage(fgImg, 0, 0, stageConfig.width, stageConfig.height);

        // Create another canvas that contains the rendered mask (false-coloring)
        const renderCanvas = document.createElement('canvas');
        renderCanvas.width = stageConfig.width;
        renderCanvas.height = stageConfig.height;


        foregroundCanvas.value = dataCanvas;
        maskDataContext.value = context;
        foregroundContext.value = renderCanvas.getContext('2d')!;
        foregroundImageConfig.image = renderCanvas;

        clear() //TODO!
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

