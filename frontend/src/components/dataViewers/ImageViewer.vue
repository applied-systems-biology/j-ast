<template>
  <div class="flex column col-grow">
    <q-toolbar class="bg-primary text-white">
      <q-btn label="Reset view" icon="fa-solid fa-expand" @click="resetLocationAndZoom"/>
    </q-toolbar>
    <div class="row col-grow" style="width: calc(100vw - 10px)">
      <div class="col-2 q-pa-lg tool-control-container">
        <div class="tool-control">
          <q-badge color="primary" class="tool-control-badge">
            <div class="label">Zoom</div>
            <q-btn size="xs" icon="fa-solid fa-undo" @click="zoom = 1">
              <q-tooltip>Reset zoom</q-tooltip>
            </q-btn>
          </q-badge>
          <q-slider
            v-model="zoom"
            :min="0.25"
            :max="3"
            :markers="0.25"
            :step="0"
            label
            marker-labels
            switch-label-side
          ></q-slider>
        </div>
      </div>
      <div class="col flex column">
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
            @wheel="onStageMouseWheel"
          >
            <konva-layer>
              <konva-image :config="backgroundImageConfig" />
            </konva-layer>
          </konva-stage>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref, useTemplateRef, watch } from 'vue';
import { KonvaEvent, MouseEventType, Position } from 'src/types/konva';
import { useQuasar } from 'quasar';
import { api } from 'boot/axios';
import { sendFailureNotification } from 'src/types/notification';
import { loadImageElementFromDataString } from 'src/types/common';

const $q = useQuasar()

const stageConfig = reactive({
  width: 16,
  height: 16,
  draggable: false,
});

const backgroundImageConfig: { image: HTMLImageElement | null } = reactive({
  image: null,
});

const props = defineProps<{
  imageBackendUrl: string;
}>()

const zoom = ref(1);

// eslint-disable-next-line @typescript-eslint/no-unused-vars
let isMouseDown = false;
// eslint-disable-next-line @typescript-eslint/no-unused-vars
let lastPosition: Position | null = null;
let isPanning: boolean = false;
let panMouseDxDy: Position | null = null;

const stage = useTemplateRef<any>('stage');

function updatePreview() {
  // Currently does nothing
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
function doTool(eventType: MouseEventType) {
  if (isPanning) {
    return;
  }
  // Do nothing
}

function onStageMouseEnter() {
  updatePreview();
  doTool(MouseEventType.MouseEnter);
}

function onStageMouseLeave() {
  updatePreview();
  doTool(MouseEventType.MouseLeave);
}

function onStageMouseDown(event: KonvaEvent<MouseEvent>) {
  if (event.evt.button == 0) {
    isMouseDown = true;
    doTool(MouseEventType.LeftMouseDown);
  } else if (event.evt.button == 1) {
    // Init the panning
    if (stage.value) {
      isPanning = true;
      const stagePos = stage.value.getStage().getPosition() as Position;
      const mousePos = { x: event.evt.x, y: event.evt.y };
      panMouseDxDy = {
        x: stagePos.x - mousePos.x,
        y: stagePos.y - mousePos.y,
      };
    }
  }
}

function onStageMouseMove(event: KonvaEvent<MouseEvent>) {
  if (isPanning) {
    if (stage.value && panMouseDxDy) {
      stage.value.getStage().position({
        x: event.evt.x + panMouseDxDy.x,
        y: event.evt.y + panMouseDxDy.y,
      });
    }
  } else {
    updatePreview();
    doTool(MouseEventType.MouseMove);
  }
}

function onStageMouseUp(event: KonvaEvent<MouseEvent>) {
  isMouseDown = false;
  isPanning = false;
  if (event.evt.button == 0) {
    doTool(MouseEventType.LeftMouseUp);
  }
}

function onStageMouseClick(event: KonvaEvent<MouseEvent>, clickCount: number) {
  if (clickCount == 1) {
    if (event.evt.button == 0) {
      doTool(MouseEventType.LeftMouseClick);
    } else if (event.evt.button == 2) {
      doTool(MouseEventType.RightMouseClick);
    }
  } else if (clickCount == 2) {
    if (event.evt.button == 0) {
      doTool(MouseEventType.LeftMouseDoubleClick);
    }
  }
}

function onStageContextMenu(event: KonvaEvent<MouseEvent>) {
  event.evt.preventDefault();
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

  node.scale({ x: newScale, y: newScale });

  const newPos = {
    x: pointer.x - mousePointTo.x * newScale,
    y: pointer.y - mousePointTo.y * newScale,
  };

  node.position(newPos);
  node.batchDraw();
}

function onZoomChanged() {
  if (stage.value) {
    const node = stage.value.getNode();
    node.scale({ x: zoom.value, y: zoom.value });
    node.batchDraw();
  }
}

function updateStageSize() {
  stageConfig.width = window.innerWidth;
  stageConfig.height = window.innerHeight;
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
function resetLocationAndZoom() {
  if (stage.value) {
    stage.value.getStage().position({ x: 0, y: 0 });
  }
  zoom.value = 1;
}

function queryFromBackend() {
  if(props.imageBackendUrl) {
    $q.loading.show({
      message: 'Loading image ...',
    });
    api.get(props.imageBackendUrl, {
      responseType: 'blob',
    })
      .then(response => {
        $q.loading.hide()
        loadImageElementFromDataString(response.data).then(backgroundImage => {
          backgroundImageConfig.image = backgroundImage;

          // const width = backgroundImage.width;
          // const height = backgroundImage.height;

          backgroundImageConfig.image = backgroundImage;
        })
      })
      .catch(() => {
        $q.loading.hide()
        sendFailureNotification("Unable to load image")
      })
  }
}

function resetTool() {
  stageConfig.draggable = true
  lastPosition = null;
  isPanning = false;
  panMouseDxDy = null;
}


onMounted(() => {
  updateStageSize();
  queryFromBackend();
  resetTool();
});
window.addEventListener('resize', updateStageSize);
watch(zoom, onZoomChanged);
watch(() => props.imageBackendUrl, queryFromBackend)

</script>
<style scoped lang="scss">
.stage-container {
  box-shadow: 0 1px 5px rgba(0, 0, 0, 0.2), 0 2px 2px rgba(0, 0, 0, 0.14),
  0 3px 1px -2px rgba(0, 0, 0, 0.12);
  flex-grow: 1;
  overflow: hidden;
  height: 0;
  margin-left: 8px !important;
}

.tool-control {
  margin-bottom: 2em;
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


</style>
<style lang="scss">
.konvajs-content {
  cursor: crosshair;
}
</style>

