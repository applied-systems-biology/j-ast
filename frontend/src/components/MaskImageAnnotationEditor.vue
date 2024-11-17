<template>
  <q-card class="q-mb-lg tool-control">
    <q-card-section class="row q-gutter-md">
      <div class="col-2">
        <q-badge color="primary" class="tool-control-badge">
          <div class="label">
            Zoom
          </div>
          <q-btn size="xs" icon="fa-solid fa-undo" @click="zoom = 1">
            <q-tooltip>Reset zoom</q-tooltip>
          </q-btn>
        </q-badge>
        <q-slider v-model="zoom" :min="0.25" :max="3" :markers="0.25" :step="0" label marker-labels
                  switch-label-side></q-slider>
      </div>
      <div class="col-2" v-if="currentToolId=='draw' || currentToolId == 'line'">
        <q-badge color="secondary" class="tool-control-badge">
          <div class="label">
            Brush size
          </div>
        </q-badge>
        <q-slider v-model="brushSize" :min="1" :step="1" :max="100" snap label :markers="10" marker-labels
                  switch-label-side></q-slider>
      </div>
      <div class="col-2" v-if="currentToolId=='polygon'">
        <q-badge color="secondary" class="tool-control-badge">
          <div class="label">
            Polygon tool
          </div>
          <q-toggle
            v-model="polygonToolDoFill"
            label="Fill"
            left-label
          />
        </q-badge>
        <div class="text-caption q-gutter-sm q-pt-sm">
          <q-badge color="cyan">
            <q-icon name="fa-solid fa-computer-mouse"/>
            Left: Add point
          </q-badge>
          <q-badge color="cyan">
            <q-icon name="fa-solid fa-computer-mouse"/>
            Right: Remove point
          </q-badge>
          <q-badge color="cyan">
            <q-icon name="fa-solid fa-computer-mouse"/>
            2xLeft: Confirm
          </q-badge>
        </div>
      </div>
    </q-card-section>
  </q-card>
  <div class="full-width stage-container">
    <konva-stage
      ref="stage"
      :config="stageConfig"
      @mousedown="onStageMouseDown"
      @mousemove="onStageMouseMove"
      @mouseup="onStageMouseUp"
      @mouseenter="onStageMouseEnter"
      @mouseleave="onStageMouseLeave"
      @click="onStageMouseClick($event, 1)"
      @dblclick="onStageMouseClick($event, 2)"
      @contextmenu="onStageContextMenu"
      @wheel="onStageMouseWheel">
      <konva-layer>
        <konva-image :config="backgroundImageConfig"/>
      </konva-layer>
      <konva-layer ref="foregroundLayer" :config="{ opacity: 0.5 }">
        <konva-image :config="foregroundImageConfig"/>
      </konva-layer>
      <konva-layer ref="previewLayer">
        <konva-circle :config="brushPreviewConfig"/>
        <konva-line :config="linePreviewConfig"/>
        <konva-line :config="polygonPreviewConfig"/>
      </konva-layer>
    </konva-stage>
  </div>
</template>
<script setup lang="ts">
import {onMounted, reactive, Ref, ref, useTemplateRef, watch} from 'vue';
import {api} from "boot/axios";
import {useQuasar} from "quasar";
import {MaskImageAnnotationPayload, loadImageElementFromDataString, uploadImage} from "src/types/common";
import {onBeforeRouteLeave} from "vue-router";
import {useEventListener} from "@vueuse/core";
import FloodFill from "q-floodfill";

const $q = useQuasar()
const imageAnnotation = defineModel<MaskImageAnnotationPayload>()
const currentToolColor: Ref<string | undefined> = defineModel<string>("tool-color")
const currentToolId: Ref<string | undefined> = defineModel<string>("tool-id")
const brushSize = ref(20)
const zoom = ref(1)
const polygonToolDoFill = ref(true)
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
  stroke: previewHighlighter,
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

const polygonPreviewConfig = reactive({
  x: 0,
  y: 0,
  points: new Array<number>(),
  closed: true,
  stroke: previewHighlighter,
  fill: previewHighlighter,
  visible: false
})

const backgroundImageConfig: { image: HTMLImageElement | null } = reactive({
  image: null
})

const foregroundImageConfig: { image: HTMLCanvasElement | null } = reactive({
  image: null
})

type KonvaEvent<T> = { evt: T }
type Position = { x: number; y: number }

enum MouseEventType {
  LeftMouseDown,
  LeftMouseUp,
  LeftMouseClick,
  RightMouseClick,
  MouseMove,
  MouseEnter,
  MouseLeave,
  LeftMouseDoubleClick
}

const stage = useTemplateRef<any>("stage")
const foregroundLayer = useTemplateRef<any>("foregroundLayer")
const previewLayer = useTemplateRef<any>("previewLayer")
const foregroundCanvas = ref<HTMLCanvasElement | null>(null);
const maskDataContext = ref<CanvasRenderingContext2D | null>(null);
const foregroundContext = ref<CanvasRenderingContext2D | null>(null);
let isEdited = false
let isMouseDown = false;
let lastPosition: Position | null = null;


function getStageMousePosition(): Position | undefined {
  if (stage.value) {
    const pointer = stage.value.getStage().getPointerPosition();
    const node = stage.value.getNode();
    if (!pointer) return undefined;

    const scale = node.scaleX();
    const position = node.position();

    return {
      x: (pointer.x - position.x) / scale,
      y: (pointer.y - position.y) / scale,
    };
  } else {
    return undefined
  }
}

function doToolDraw() {

  const context = maskDataContext.value

  if (!stage.value || !foregroundLayer.value || !context) {
    return
  }

  const pos = getStageMousePosition()

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

function doToolPolygon(eventType: MouseEventType) {
  if (!stage.value || !foregroundLayer.value) {
    return
  }
  const pos = getStageMousePosition()
  if (!pos) {
    return;
  }
  polygonPreviewConfig.visible = true
  if(eventType == MouseEventType.MouseMove) {
    if(polygonPreviewConfig.points.length > 1) {
      polygonPreviewConfig.points[polygonPreviewConfig.points.length - 2] = pos.x
      polygonPreviewConfig.points[polygonPreviewConfig.points.length - 1] = pos.y
      previewLayer.value.getNode().batchDraw()
    }
  }
  else if(eventType == MouseEventType.LeftMouseClick) {
    if(polygonPreviewConfig.points.length == 0) {
      // Add also the starting point
      polygonPreviewConfig.points.push(pos.x)
      polygonPreviewConfig.points.push(pos.y)
    }
    polygonPreviewConfig.points.push(pos.x)
    polygonPreviewConfig.points.push(pos.y)
    previewLayer.value.getNode().batchDraw()

  }
  else if(eventType == MouseEventType.RightMouseClick) {
    if(polygonPreviewConfig.points.length > 1 ) {
      polygonPreviewConfig.points.splice(polygonPreviewConfig.points.length - 2, 2)

      // Update the last pos
      if(polygonPreviewConfig.points.length > 1) {
        polygonPreviewConfig.points[polygonPreviewConfig.points.length - 2] = pos.x
        polygonPreviewConfig.points[polygonPreviewConfig.points.length - 1] = pos.y
      }

      previewLayer.value.getNode().batchDraw()
    }
  }
  else if(eventType == MouseEventType.LeftMouseDoubleClick) {
    // Commit
    if(polygonPreviewConfig.points.length >= 4) {

      const context = maskDataContext.value
      if (!context) {
        return;
      }

      context.imageSmoothingEnabled = false
      context.strokeStyle = currentToolColor.value!
      context.fillStyle = currentToolColor.value!
      context.globalCompositeOperation = "source-over"
      context.lineCap = "round"
      context.lineJoin = "round"
      context.lineWidth = 1
      context.beginPath();
      context.moveTo(polygonPreviewConfig.points[0], polygonPreviewConfig.points[1])
      for (let i = 2; i < polygonPreviewConfig.points.length; i+=2) {
        context.lineTo(polygonPreviewConfig.points[i], polygonPreviewConfig.points[i + 1])
      }
      context.closePath();
      if(polygonToolDoFill.value) {
        context.fill()
      }
      else {
        context.stroke()
      }

      renderMaskToForeground()
      isEdited = true

      // Reset
      polygonPreviewConfig.points = []
    }
    previewLayer.value.getNode().batchDraw()
  }
}

function doToolLine(eventType: MouseEventType) {
  if (!stage.value || !foregroundLayer.value) {
    return
  }
  const pos = getStageMousePosition()
  if (!pos) {
    return;
  }
  if (eventType == MouseEventType.LeftMouseDown) {
    lastPosition = pos as Position
    linePreviewConfig.strokeWidth = brushSize.value
  } else if (eventType == MouseEventType.MouseMove) {
    if (isMouseDown && lastPosition) {
      linePreviewConfig.visible = true
      linePreviewConfig.points = [lastPosition.x, lastPosition.y, pos.x, pos.y]
    }
  } else if (eventType == MouseEventType.LeftMouseUp) {
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
  const pos = getStageMousePosition()
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
  floodFill.fill(currentToolColor.value!, Math.floor(pos.x), Math.floor(pos.y), 0)
  context.putImageData(floodFill.imageData, 0, 0)

  renderMaskToForeground()
  isEdited = true
}

function updatePreview() {
  if (!stage.value || !foregroundLayer.value) {
    return
  }

  const pos = getStageMousePosition()
  if (!pos) {
    brushPreviewConfig.visible = false
    return;
  }

  brushPreviewConfig.visible = false
  linePreviewConfig.visible = false
  polygonPreviewConfig.visible = false

  if (currentToolId.value === "draw" || currentToolId.value === "line") {
    brushPreviewConfig.x = pos.x
    brushPreviewConfig.y = pos.y
    brushPreviewConfig.visible = true
    brushPreviewConfig.radius = brushSize.value / 2
  } else if(currentToolId.value === "polygon") {
    polygonPreviewConfig.visible = true;
  }
}

function doTool(eventType: MouseEventType) {
  switch (currentToolId.value) {
    case "draw": {
      if (eventType == MouseEventType.LeftMouseDown) {
        doToolDraw()
      } else if (eventType == MouseEventType.MouseMove) {
        if (isMouseDown) {
          doToolDraw()
        }
      } else if (eventType == MouseEventType.LeftMouseUp) {
        lastPosition = null
      }
    }
      break
    case "line": {
      doToolLine(eventType)
    }
    break;
    case "polygon": {
      doToolPolygon(eventType)
    }
    break;
    case "fill": {
      if (eventType == MouseEventType.LeftMouseClick) {
        doToolFill()
      }
    }
  }
}

function resetTool() {
  stageConfig.draggable = currentToolId.value == "pan"
  lastPosition = null
  polygonPreviewConfig.points = []
}

/**
 * Applies internal thresholding on the mask data, which may be needed for some operations
 * Also done before uploading
 * No need to render afterwards, as the renderer uses the same algorithm
 */
function doThresholding() {
  const context = maskDataContext.value
  if (!context) {
    return;
  }
  const imageData = context.getImageData(0, 0, context.canvas.width, context.canvas.height)
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
  if (!srcContext || !targetContext) {
    return
  }
  const imageData = srcContext.getImageData(0, 0, srcContext.canvas.width, srcContext.canvas.height)
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

function onStageMouseDown(event: KonvaEvent<MouseEvent>) {
  if (event.evt.button == 0) {
    isMouseDown = true
    doTool(MouseEventType.LeftMouseDown)
  }
}

function onStageMouseMove() {
  updatePreview()
  doTool(MouseEventType.MouseMove)
}

function onStageMouseUp(event: KonvaEvent<MouseEvent>) {
  isMouseDown = false
  if (event.evt.button == 0) {
    doTool(MouseEventType.LeftMouseUp)
  }
}

function onStageMouseClick(event: KonvaEvent<MouseEvent>, clickCount: number) {
  if (clickCount == 1) {
    if (event.evt.button == 0) {
      doTool(MouseEventType.LeftMouseClick)
    }
    else if (event.evt.button == 2) {
      doTool(MouseEventType.RightMouseClick)
    }
  } else if (clickCount == 2) {
    if (event.evt.button == 0) {
      doTool(MouseEventType.LeftMouseDoubleClick)
    }
  }
}

function onStageMouseWheel(event: KonvaEvent<WheelEvent>) {
  event.evt.preventDefault();
  if (!stage.value) {
    return;
  }
  const node = stage.value.getNode();
  const oldScale = node.scaleX();
  const pointer = node.getPointerPosition();
  if (!pointer) return;

  // Determine the new scale based on wheel delta
  const scaleBy = 1.1;
  const direction = event.evt.deltaY > 0 ? 1 : -1;
  const newScale = direction > 0 ? oldScale / scaleBy : oldScale * scaleBy;

  // Limit the zoom level
  zoom.value = Math.min(3, Math.max(0.25, newScale));

  // Calculate the new position to zoom into the pointer location
  const mousePointTo = {
    x: (pointer.x - node.x()) / oldScale,
    y: (pointer.y - node.y()) / oldScale,
  };

  node.scale({x: newScale, y: newScale});

  const newPos = {
    x: pointer.x - mousePointTo.x * newScale,
    y: pointer.y - mousePointTo.y * newScale,
  };

  node.position(newPos);
  node.batchDraw();
}

function onStageContextMenu(event: KonvaEvent<MouseEvent>) {
  event.evt.preventDefault()
}

function saveImage() {
  if (maskDataContext.value) {
    doThresholding()
    const pngData = maskDataContext.value.canvas.toDataURL('image/png')
    $q.loading.show({message: "Uploading image data ..."})
    uploadImage(`/mask-image-annotation/${imageAnnotation.value?.imageId}/${imageAnnotation.value?.annotationTypeId}/raw`, pngData)
      .then(() => {
        $q.notify({
          type: "positive",
          message: "Annotation was successfully uploaded",
        })
        isEdited = false
      })
      .catch(() => {
        $q.notify({
          type: "negative",
          message: "Error while uploading!",
        })
      })
      .finally(() => {
        $q.loading.hide();
      })
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

function onZoomChanged() {
  if (stage.value) {
    const node = stage.value.getNode();
    node.scale({x: zoom.value, y: zoom.value});
    node.batchDraw();
  }
}

watch(zoom, onZoomChanged)

function queryFromBackend() {
  $q.loading.show({
    message: "Preparing image editor ..."
  });
  if (imageAnnotation.value && imageAnnotation.value.imageId >= 0) {

    api.get(`/image/${imageAnnotation.value.imageId}/raw`, {responseType: 'blob'}).then(backgroundResponse => {
      loadImageElementFromDataString(backgroundResponse.data).then((backgroundImage) => {
        backgroundImageConfig.image = backgroundImage;

        const width = backgroundImage.width;
        const height = backgroundImage.height;

        // Create a canvas that only holds the mask data
        const dataCanvas = document.createElement('canvas');
        dataCanvas.width = width;
        dataCanvas.height = height;
        const context = dataCanvas.getContext('2d')!;
        // context.drawImage(fgImg, 0, 0, stageConfig.width, stageConfig.height);

        // Create another canvas that contains the rendered mask (false-coloring)
        const renderCanvas = document.createElement('canvas');
        renderCanvas.width = width;
        renderCanvas.height = height;

        foregroundCanvas.value = dataCanvas;
        maskDataContext.value = context;
        foregroundContext.value = renderCanvas.getContext('2d')!;
        foregroundImageConfig.image = renderCanvas;

        api.get(`/mask-image-annotation/${imageAnnotation.value?.imageId}/${imageAnnotation.value?.annotationTypeId}/raw`, {responseType: 'blob'}).then(foregroundResponse => {
          loadImageElementFromDataString(foregroundResponse.data).then((foregroundImage) => {
            context.drawImage(foregroundImage, 0, 0, foregroundImage.width, foregroundImage.height);
            isEdited = false
            renderMaskToForeground()
            $q.loading.hide();
          })
        })
      })
    })
  }
}

function updateStageSize() {
  stageConfig.width = window.innerWidth;
  stageConfig.height = window.innerHeight;
}

function resetLocationAndZoom() {
  if (stage.value) {
    stage.value.getStage().position({x: 0, y: 0});
  }
  zoom.value = 1
}

onMounted(() => {
  updateStageSize()
  queryFromBackend()
  resetTool()
})
window.addEventListener('resize', () => {
  updateStageSize()
})
watch(imageAnnotation, () => {
  queryFromBackend()
})
watch(currentToolId, () => {
  resetTool()
})
defineExpose({
  saveImage,
  queryFromBackend,
  clear,
  resetLocationAndZoom
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
<style scoped lang="scss">
.tool-control {
  height: 8em;
}

.tool-control-badge {
  display: flex;
  flex-direction: row;
  gap: 3px;
  height: 3em;

  .label {
    flex-grow: 1;
  }
}

.stage-container {
  box-shadow: 0 1px 5px rgba(0, 0, 0, 0.2), 0 2px 2px rgba(0, 0, 0, 0.14), 0 3px 1px -2px rgba(0, 0, 0, 0.12);
  flex-grow: 1;
  overflow: hidden;
  height: 0;
  margin-left: 8px !important;
}
</style>
<style lang="scss">
.konvajs-content {
  cursor: crosshair;
}
</style>

