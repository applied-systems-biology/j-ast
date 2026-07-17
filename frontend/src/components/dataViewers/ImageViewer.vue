<!--
  - Copyright (c) 2026.
  -
  - Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
  - https://www.leibniz-hki.de/en/applied-systems-biology.html
  - HKI-Center for Systems Biology of Infection
  - Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
  - Adolf-Reichwein-Straße 23, 07745 Jena, Germany
  -
  - The project code is licensed under MIT.
  - See the LICENSE file provided with the code for the full license.
  -->

<template>
  <div class="flex column col-grow">
    <q-toolbar class="bg-primary text-white edit-toolbar">
      <q-btn no-caps no-wrap label="Download" icon="fa-solid fa-download" color="green" @click="download"/>
      <q-btn no-caps no-wrap label="Reset view" icon="fa-solid fa-expand" color="blue" @click="resetLocationAndZoom"/>
      <ZoomControl v-model="zoom" />
      <slot name="toolbar"></slot>
    </q-toolbar>
    <div class="row col-grow" style="width: calc(100vw - 10px)">
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
import { downloadFromApi, ensureExtension, loadImageElementFromDataString } from 'src/types/common';
import ZoomControl from 'components/utils/ZoomControl.vue';

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
  filename: string;
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

function download() {
  downloadFromApi(
    props.imageBackendUrl,
    ensureExtension(props.filename, ['.png'])
  );
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

