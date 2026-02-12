<template>
  <q-layout view="hHh lpR fFf">
    <q-header>
      <q-toolbar>
        <q-toolbar-title class="row items-center q-gutter-sm">
          <HeaderLogoButtonComponent/>
          <div>/</div>
          <q-skeleton
              v-if="!projectPayload.name"
              style="width: 200px"
              type="text"
          />
          <router-link
              v-else
              :to="`/project/${imagePayload.projectId}`"
              style="text-decoration: underline; color: inherit"
          >{{ projectPayload.name }}
          </router-link>
          <div>/</div>
          <q-skeleton
              v-if="!imagePayload.fileName"
              style="width: 200px"
              type="text"
          />
          <div v-else>{{ imagePayload.fileName }}</div>
          <div>/</div>
          <div>{{ annotationName }}</div>
          <q-btn color="green" icon="upload" no-caps no-wrap size="lg" @click="postToBackend">Save annotation
          </q-btn>
        </q-toolbar-title>
        <UserManagerComponent/>
        <DocumentationComponent/>
      </q-toolbar>
      <q-toolbar class="bg-primary text-white edit-toolbar">
        <q-btn-toggle
            no-caps no-wrap
            v-model="currentAnnotationColorId"
            :options="annotationColors"
            color="blue-grey"
            toggle-color="green"
        >
          <q-tooltip
          >Determines whether the foreground or the background is drawn
          </q-tooltip>
        </q-btn-toggle>
        <q-btn-toggle
            no-caps no-wrap
            v-model="currentAnnotationToolId"
            :options="annotationTools"
            color="blue-grey"
            toggle-color="green"
        >
          <q-tooltip>The tool to draw the foreground/background</q-tooltip>
        </q-btn-toggle>
        <q-btn no-caps no-wrap color="blue-grey" icon="fa-solid fa-gear" label="Tools">
          <q-menu>
            <q-list style="min-width: 100px">
              <q-item v-close-popup clickable @click="resetLocationAndZoom">
                <q-item-section avatar>
                  <q-icon name="fa-solid fa-expand"/>
                </q-item-section>
                <q-item-section>Reset view</q-item-section>
              </q-item>
              <q-separator/>
              <q-item
                  v-close-popup
                  clickable
                  @click="onUserRequestClearAnnotation"
              >
                <q-item-section avatar>
                  <q-icon name="fa-solid fa-eraser"/>
                </q-item-section>
                <q-item-section>Clear</q-item-section>
              </q-item>
              <q-item
                  v-close-popup
                  clickable
                  @click="onUserRequestRestoreSavedState"
              >
                <q-item-section avatar>
                  <q-icon name="fa-solid fa-undo"/>
                </q-item-section>
                <q-item-section>Restore saved state</q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </q-btn>
        <q-btn no-caps no-wrap color="blue-grey" icon="fa-solid fa-download" label="Download">
          <q-menu>
            <q-list style="min-width: 100px">
              <q-item v-close-popup clickable @click="downloadRaw">
                <q-item-section avatar>
                  <q-icon name="fa-solid fa-image"/>
                </q-item-section>
                <q-item-section>Raw image</q-item-section>
              </q-item>
              <q-separator/>
              <q-item v-close-popup clickable @click="downloadMask">
                <q-item-section avatar>
                  <q-icon name="fa-solid fa-image"/>
                </q-item-section>
                <q-item-section>Mask</q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </q-btn>
      </q-toolbar>
    </q-header>
    <q-drawer
        :model-value="true"
        class="q-pa-lg tool-control-container"
        elevated
        side="left"
    >
      <div class="tool-control">
        <q-badge class="tool-control-badge" color="primary">
          <div class="label">Zoom</div>
          <q-btn no-caps no-wrap icon="fa-solid fa-undo" size="xs" @click="zoom = 1">
            <q-tooltip>Reset zoom</q-tooltip>
          </q-btn>
        </q-badge>
        <q-slider
            v-model="zoom"
            :marker-labels="zoomLabels"
            :markers="0.25"
            :max="3"
            :min="0.25"
            :step="0"
            label
            switch-label-side
        ></q-slider>
      </div>
      <div
          v-if="
          currentAnnotationToolId == 'draw' || currentAnnotationToolId == 'line'
        "
          class="tool-control"
      >
        <q-badge class="tool-control-badge" color="secondary">
          <div class="label">Brush size</div>
        </q-badge>
        <q-slider
            v-model="brushSize"
            :markers="50"
            :max="250"
            :min="1"
            :step="1"
            label
            marker-labels
            snap
            switch-label-side
        ></q-slider>
      </div>
      <div v-if="currentAnnotationToolId == 'polygon'" class="tool-control">
        <q-badge class="tool-control-badge" color="secondary">
          <div class="label">Polygon tool</div>
          <q-toggle v-model="polygonToolDoFill" label="Fill" left-label/>
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
      <div v-if="currentAnnotationToolId == 'symmetry'" class="tool-control">
        <q-badge class="tool-control-badge" color="secondary">
          <div class="label">Symmetry tool</div>
        </q-badge>
        <div class="text-caption q-gutter-sm q-pt-sm">
          <q-option-group
              v-model="symmetryToolMode"
              :options="symmetryToolModes"
          />
        </div>
      </div>
      <div v-if="isEdited">
        <q-badge class="tool-control-badge" color="secondary">
          <div class="label">Unsaved changes</div>
          <q-btn color="red" icon="undo" size="xs" @click="queryFromBackend"/>
          <q-btn color="green" icon="upload" size="xs" @click="postToBackend"/>
        </q-badge>
        <div class="text-caption q-gutter-sm q-pa-sm q-pt-md">
          Please remember to save your changes. Otherwise, they will be
          lost.
        </div>
      </div>
    </q-drawer>
    <q-page-container>
      <q-page class="flex column q-gutter-sm" padding>
        <div class="full-width stage-container">
          <konva-stage
              ref="stage"
              :config="stageConfig"
              @click="onStageMouseClick($event, 1)"
              @contextmenu="onStageContextMenu"
              @dblclick="onStageMouseClick($event, 2)"
              @mousedown="onStageMouseDown"
              @mouseenter="onStageMouseEnter"
              @mouseleave="onStageMouseLeave"
              @mousemove="onStageMouseMove"
              @mouseup="onStageMouseUp"
              @wheel="onStageMouseWheel"
          >
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
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script lang="ts" setup>
import UserManagerComponent from 'components/layout/UserManagerComponent.vue';
import HeaderLogoButtonComponent from 'components/layout/HeaderLogoButtonComponent.vue';
import {computed, onMounted, reactive, Ref, ref, useTemplateRef, watch,} from 'vue';
import {
  downloadDataString,
  downloadFromApi,
  ensureExtension,
  loadImageElementFromDataString,
  removeExtensionIfPresent,
  uploadImage,
} from 'src/types/common';
import {api} from 'boot/axios';
import {plainToInstance} from 'class-transformer';
import {onBeforeRouteLeave, useRoute} from 'vue-router';
import {useQuasar} from 'quasar';
import FloodFill from 'q-floodfill';
import {useEventListener} from '@vueuse/core';
import {sendFailureNotification, sendSuccessNotification,} from 'src/types/notification';
import {onDialogYes} from 'src/types/dialog';
import {ProjectMetadataPayload} from 'src/types/project';
import {AssayType} from 'src/types/assayType';
import {ImagePayload, MaskImageAnnotationPayload} from 'src/types/image';
import {mirrorImageData, MirrorOperationMode} from 'src/types/drawingMirror';
import {KonvaEvent, MouseEventType, Position} from 'src/types/konva';
import DocumentationComponent from "components/layout/DocumentationComponent.vue";

const $q = useQuasar();
const $route = useRoute();
const imageId = $route.params.imageId;
const annotationTypeId = $route.params.annotationTypeId;
const brushSize = ref(20);
const zoom = ref(1);
const polygonToolDoFill = ref(true);
const symmetryToolMode = ref(MirrorOperationMode.Max);
const previewHighlighter = '#00ffffaa';

const stageConfig = reactive({
  width: 16,
  height: 16,
  draggable: false,
});

const brushPreviewConfig = reactive({
  x: 0,
  y: 0,
  radius: 1,
  stroke: previewHighlighter,
  visible: false,
});

const linePreviewConfig = reactive({
  x: 0,
  y: 0,
  strokeWidth: 1,
  lineCap: 'round',
  lineJoin: 'round',
  points: [0, 0, 100, 100],
  stroke: previewHighlighter,
  visible: false,
});

const polygonPreviewConfig = reactive({
  x: 0,
  y: 0,
  points: new Array<number>(),
  closed: true,
  stroke: previewHighlighter,
  fill: previewHighlighter,
  visible: false,
});

const backgroundImageConfig: { image: HTMLImageElement | null } = reactive({
  image: null,
});

const foregroundImageConfig: { image: HTMLCanvasElement | null } = reactive({
  image: null,
});


const stage = useTemplateRef<any>('stage');
const foregroundLayer = useTemplateRef<any>('foregroundLayer');
const previewLayer = useTemplateRef<any>('previewLayer');
const foregroundCanvas = ref<HTMLCanvasElement | null>(null);
const maskDataContext = ref<CanvasRenderingContext2D | null>(null);
const foregroundContext = ref<CanvasRenderingContext2D | null>(null);
const isEdited = ref(false);
let isMouseDown = false;
let lastPosition: Position | null = null;
let isPanning: boolean = false;
let panMouseDxDy: Position | null = null;

defineOptions({
  name: 'MaskImageAnnotationLayout',
});

const annotationTools = [
  {label: '', value: 'pan', icon: 'fa-solid fa-hand'},
  {label: '', value: 'draw', icon: 'fa-solid fa-pencil'},
  {label: '', value: 'polygon', icon: 'fa-solid fa-draw-polygon'},
  {label: '', value: 'line', icon: 'fa-solid fa-slash'},
  {label: '', value: 'symmetry', icon: 'fa-solid fa-percent fa-rotate-90'},
  {label: '', value: 'fill', icon: 'fa-solid fa-fill-drip'},
];
const annotationColors = [
  {label: 'Foreground', value: '#FFFFFF', icon: 'fa-solid fa-square'},
  {label: 'Background', value: '#000000', icon: 'fa-solid fa-eraser'},
];
const symmetryToolModes = [
  {
    label: 'Max',
    value: MirrorOperationMode.Max,
    icon: '',
  },
  {
    label: 'Min',
    value: MirrorOperationMode.Min,
    icon: '',
  },
  {
    label: 'Above/Left',
    value: MirrorOperationMode.AboveOrLeft,
    icon: '',
  },
  {
    label: 'Below/Right',
    value: MirrorOperationMode.BelowOrRight,
    icon: '',
  },
];
const currentAnnotationToolId = ref('draw');
const currentAnnotationColorId = ref('#FFFFFF');

const imagePayload: Ref<ImagePayload> = ref(new ImagePayload());
const projectPayload: Ref<ProjectMetadataPayload> = ref(
    new ProjectMetadataPayload()
);
const annotationPayload: Ref<MaskImageAnnotationPayload> = ref(
    new MaskImageAnnotationPayload()
);

const annotationName = computed(() => {
  switch (annotationTypeId) {
    case 'plate':
      return 'Plate';
    case 'strip-disk':
      if (imagePayload.value.assayType == AssayType.ETest) {
        return 'ETest strip';
      } else if (imagePayload.value.assayType == AssayType.DDA) {
        return 'DDA disk';
      }
      break;
    case 'zoi-shape':
      return 'ZOI';
  }
  return annotationTypeId;
});

function zoomLabels(value: number) {
  return value == 1 ? "100%" : " "
}

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
    return undefined;
  }
}

function doToolDraw() {
  const context = maskDataContext.value;

  if (!stage.value || !foregroundLayer.value || !context) {
    return;
  }

  const pos = getStageMousePosition();

  if (!pos) {
    return;
  }
  if (!lastPosition) {
    lastPosition = pos as Position;
  }

  context.imageSmoothingEnabled = false;
  context.strokeStyle = currentAnnotationColorId.value!;
  context.globalCompositeOperation = 'source-over';
  context.lineCap = 'round';
  context.lineJoin = 'round';
  context.lineWidth = brushSize.value;
  context.beginPath();
  context.moveTo(lastPosition.x, lastPosition.y);
  context.lineTo(pos.x, pos.y);
  context.stroke();
  lastPosition = pos as Position;

  renderMaskToForeground();
  isEdited.value = true;
}

function doToolPolygon(eventType: MouseEventType) {
  if (!stage.value || !foregroundLayer.value) {
    return;
  }
  const pos = getStageMousePosition();
  if (!pos) {
    return;
  }
  polygonPreviewConfig.visible = true;
  if (eventType == MouseEventType.MouseMove) {
    if (polygonPreviewConfig.points.length > 1) {
      polygonPreviewConfig.points[polygonPreviewConfig.points.length - 2] =
          pos.x;
      polygonPreviewConfig.points[polygonPreviewConfig.points.length - 1] =
          pos.y;
      previewLayer.value.getNode().batchDraw();
    }
  } else if (eventType == MouseEventType.LeftMouseClick) {
    if (polygonPreviewConfig.points.length == 0) {
      // Add also the starting point
      polygonPreviewConfig.points.push(pos.x);
      polygonPreviewConfig.points.push(pos.y);
    }
    polygonPreviewConfig.points.push(pos.x);
    polygonPreviewConfig.points.push(pos.y);
    previewLayer.value.getNode().batchDraw();
  } else if (eventType == MouseEventType.RightMouseClick) {
    if (polygonPreviewConfig.points.length > 1) {
      polygonPreviewConfig.points.splice(
          polygonPreviewConfig.points.length - 2,
          2
      );

      // Update the last pos
      if (polygonPreviewConfig.points.length > 1) {
        polygonPreviewConfig.points[polygonPreviewConfig.points.length - 2] =
            pos.x;
        polygonPreviewConfig.points[polygonPreviewConfig.points.length - 1] =
            pos.y;
      }

      previewLayer.value.getNode().batchDraw();
    }
  } else if (eventType == MouseEventType.LeftMouseDoubleClick) {
    // Commit
    if (polygonPreviewConfig.points.length >= 4) {
      const context = maskDataContext.value;
      if (!context) {
        return;
      }

      context.imageSmoothingEnabled = false;
      context.strokeStyle = currentAnnotationColorId.value!;
      context.fillStyle = currentAnnotationColorId.value!;
      context.globalCompositeOperation = 'source-over';
      context.lineCap = 'round';
      context.lineJoin = 'round';
      context.lineWidth = 1;
      context.beginPath();
      context.moveTo(
          polygonPreviewConfig.points[0],
          polygonPreviewConfig.points[1]
      );
      for (let i = 2; i < polygonPreviewConfig.points.length; i += 2) {
        context.lineTo(
            polygonPreviewConfig.points[i],
            polygonPreviewConfig.points[i + 1]
        );
      }
      context.closePath();
      if (polygonToolDoFill.value) {
        context.fill();
      } else {
        context.stroke();
      }

      renderMaskToForeground();
      isEdited.value = true;

      // Reset
      polygonPreviewConfig.points = [];
    }
    previewLayer.value.getNode().batchDraw();
  }
}

function doToolLine(eventType: MouseEventType) {
  if (!stage.value || !foregroundLayer.value) {
    return;
  }
  const pos = getStageMousePosition();
  if (!pos) {
    return;
  }
  if (eventType == MouseEventType.LeftMouseDown) {
    lastPosition = pos as Position;
    linePreviewConfig.strokeWidth = brushSize.value;
  } else if (eventType == MouseEventType.MouseMove) {
    if (isMouseDown && lastPosition) {
      linePreviewConfig.visible = true;
      linePreviewConfig.points = [lastPosition.x, lastPosition.y, pos.x, pos.y];
    }
  } else if (eventType == MouseEventType.LeftMouseUp) {
    linePreviewConfig.visible = false;
    if (lastPosition) {
      const context = maskDataContext.value;
      if (!context) {
        return;
      }

      context.imageSmoothingEnabled = false;
      context.strokeStyle = currentAnnotationColorId.value!;
      context.globalCompositeOperation = 'source-over';
      context.lineCap = 'round';
      context.lineJoin = 'round';
      context.lineWidth = brushSize.value;
      context.beginPath();
      context.moveTo(lastPosition.x, lastPosition.y);
      context.lineTo(pos.x, pos.y);
      context.stroke();

      renderMaskToForeground();
      isEdited.value = true;
    }
  }
}

function doToolSymmetry(eventType: MouseEventType) {
  if (!stage.value || !foregroundLayer.value) {
    return;
  }
  const pos = getStageMousePosition();
  if (!pos) {
    return;
  }
  if (eventType == MouseEventType.LeftMouseDown) {
    lastPosition = pos as Position;
    linePreviewConfig.strokeWidth = 1;
  } else if (eventType == MouseEventType.MouseMove) {
    if (isMouseDown && lastPosition) {
      linePreviewConfig.visible = true;
      linePreviewConfig.points = [lastPosition.x, lastPosition.y, pos.x, pos.y];
    }
  } else if (eventType == MouseEventType.LeftMouseUp) {
    linePreviewConfig.visible = false;
    if (lastPosition) {
      const context = maskDataContext.value;
      if (!context) {
        return;
      }

      mirrorImageData(
          context.canvas,
          pos.x,
          pos.y,
          lastPosition.x,
          lastPosition.y,
          symmetryToolMode.value!
      );

      renderMaskToForeground();
      isEdited.value = true;
    }
  }
}

function doToolFill() {
  if (!stage.value || !foregroundLayer.value) {
    return;
  }
  const pos = getStageMousePosition();
  if (!pos) {
    return;
  }
  const context = maskDataContext.value;
  if (!context) {
    return;
  }

  // Needed to handle antialiasing
  doThresholding();

  const canvas = context.canvas;
  const imageData = context.getImageData(0, 0, canvas.width, canvas.height);

  const floodFill = new FloodFill(imageData);
  floodFill.fill(
      currentAnnotationColorId.value!,
      Math.floor(pos.x),
      Math.floor(pos.y),
      0
  );
  context.putImageData(floodFill.imageData, 0, 0);

  renderMaskToForeground();
  isEdited.value = true;
}

function updatePreview() {
  if (!stage.value || !foregroundLayer.value) {
    return;
  }

  const pos = getStageMousePosition();
  if (!pos) {
    brushPreviewConfig.visible = false;
    return;
  }

  brushPreviewConfig.visible = false;
  linePreviewConfig.visible = false;
  polygonPreviewConfig.visible = false;

  if (
      currentAnnotationToolId.value === 'draw' ||
      currentAnnotationToolId.value === 'line'
  ) {
    brushPreviewConfig.x = pos.x;
    brushPreviewConfig.y = pos.y;
    brushPreviewConfig.visible = true;
    brushPreviewConfig.radius = brushSize.value / 2;
  } else if (currentAnnotationToolId.value === 'polygon') {
    polygonPreviewConfig.visible = true;
    brushPreviewConfig.x = pos.x;
    brushPreviewConfig.y = pos.y;
    brushPreviewConfig.visible = true;
    brushPreviewConfig.radius = 1;
  }
}

function doTool(eventType: MouseEventType) {
  if (isPanning) {
    return;
  }
  switch (currentAnnotationToolId.value) {
    case 'draw': {
      if (eventType == MouseEventType.LeftMouseDown) {
        doToolDraw();
      } else if (eventType == MouseEventType.MouseMove) {
        if (isMouseDown) {
          doToolDraw();
        }
      } else if (eventType == MouseEventType.LeftMouseUp) {
        lastPosition = null;
      }
    }
      break;
    case 'line': {
      doToolLine(eventType);
    }
      break;
    case 'symmetry': {
      doToolSymmetry(eventType);
    }
      break;
    case 'polygon': {
      doToolPolygon(eventType);
    }
      break;
    case 'fill': {
      if (eventType == MouseEventType.LeftMouseClick) {
        doToolFill();
      }
    }
  }
}

function resetTool() {
  stageConfig.draggable = currentAnnotationToolId.value == 'pan';
  lastPosition = null;
  isPanning = false;
  panMouseDxDy = null;
  polygonPreviewConfig.points = [];
}

/**
 * Applies internal thresholding on the mask data, which may be needed for some operations
 * Also done before uploading
 * No need to render afterwards, as the renderer uses the same algorithm
 */
function doThresholding() {
  const context = maskDataContext.value;
  if (!context) {
    return;
  }
  const imageData = context.getImageData(
      0,
      0,
      context.canvas.width,
      context.canvas.height
  );
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

  context.putImageData(imageData, 0, 0);
}

function renderMaskToForeground() {
  const srcContext = maskDataContext.value;
  const targetContext = foregroundContext.value;
  if (!srcContext || !targetContext) {
    return;
  }
  const imageData = srcContext.getImageData(
      0,
      0,
      srcContext.canvas.width,
      srcContext.canvas.height
  );
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

  targetContext.putImageData(imageData, 0, 0);
  foregroundLayer.value.getNode().batchDraw();
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
      const mousePos = {x: event.evt.x, y: event.evt.y};
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
  event.evt.preventDefault();
}

function getMaskAsDataString(): string | undefined {
  if (maskDataContext.value) {
    doThresholding();
    return maskDataContext.value.canvas.toDataURL('image/png');
  }
  return undefined;
}

function postToBackend() {
  if (maskDataContext.value) {
    doThresholding();
    const pngData = maskDataContext.value.canvas.toDataURL('image/png');
    $q.loading.show({message: 'Uploading image data ...'});
    uploadImage(
        `/mask-image-annotation/${annotationPayload.value?.imageId}/${annotationPayload.value?.annotationTypeId}/raw`,
        pngData
    )
        .then(() => {
          sendSuccessNotification('Annotation was successfully uploaded');
          isEdited.value = false;
        })
        .catch(() => {
          sendFailureNotification('Error while uploading!');
        })
        .finally(() => {
          $q.loading.hide();
        });
  }
}

function clear() {
  const context = maskDataContext.value;
  if (!stage.value || !foregroundLayer.value || !context) {
    return;
  }

  context.imageSmoothingEnabled = false;
  context.fillStyle = 'black';
  context.globalCompositeOperation = 'source-over';
  context.fillRect(0, 0, context.canvas.width, context.canvas.height);
  renderMaskToForeground();

  isEdited.value = true;
}

function onZoomChanged() {
  if (stage.value) {
    const node = stage.value.getNode();
    node.scale({x: zoom.value, y: zoom.value});
    node.batchDraw();
  }
}

function downloadRaw() {
  $q.loading.show({message: 'Preparing ...'});
  downloadFromApi(
      `/image/${imageId}/raw`,
      ensureExtension(imagePayload.value.fileName, ['.png'])
  ).finally(() => {
    $q.loading.hide();
  });
}

function downloadMask() {
  downloadDataString(
      getMaskAsDataString()!,
      removeExtensionIfPresent(imagePayload.value.fileName) +
      '_' +
      annotationPayload.value.annotationTypeId +
      '.png'
  );
}

function onUserRequestRestoreSavedState() {
  onDialogYes(
      'Restore from saved annotation',
      'Do you really want to reset the annotation from the saved state?'
  ).then(queryFromBackend);
}

function onUserRequestClearAnnotation() {
  onDialogYes(
      'Clear annotation',
      'Do you really want to erase the annotation?'
  ).then(clear);
}

function queryFromBackend() {
  $q.loading.show({
    message: 'Preparing image editor ...',
  });

  api.get(`/image/${imageId}`).then((response) => {
    imagePayload.value = plainToInstance(ImagePayload, response.data);
    api
        .get<ProjectMetadataPayload>(`/project/${imagePayload.value.projectId}`)
        .then((response) => {
          projectPayload.value = plainToInstance(
              ProjectMetadataPayload,
              response.data
          );
        });
    api
        .get(`/mask-image-annotation/${imageId}/${annotationTypeId}`)
        .then((response) => {
          annotationPayload.value = plainToInstance(
              MaskImageAnnotationPayload,
              response.data
          );

          // Load the image
          if (annotationPayload.value && annotationPayload.value.imageId >= 0) {
            api
                .get(`/image/${annotationPayload.value.imageId}/raw`, {
                  responseType: 'blob',
                })
                .then((backgroundResponse) => {
                  loadImageElementFromDataString(backgroundResponse.data).then(
                      (backgroundImage) => {
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

                        api
                            .get(
                                `/mask-image-annotation/${annotationPayload.value?.imageId}/${annotationPayload.value?.annotationTypeId}/raw`,
                                {responseType: 'blob'}
                            )
                            .then((foregroundResponse) => {
                              loadImageElementFromDataString(
                                  foregroundResponse.data
                              ).then((foregroundImage) => {
                                context.drawImage(
                                    foregroundImage,
                                    0,
                                    0,
                                    foregroundImage.width,
                                    foregroundImage.height
                                );
                                isEdited.value = false;
                                renderMaskToForeground();
                                $q.loading.hide();
                              });
                            });
                      }
                  );
                });
          }
        });
  });
}

function updateStageSize() {
  stageConfig.width = window.innerWidth;
  stageConfig.height = window.innerHeight;
}

function resetLocationAndZoom() {
  if (stage.value) {
    stage.value.getStage().position({x: 0, y: 0});
  }
  zoom.value = 1;
}

onMounted(() => {
  updateStageSize();
  queryFromBackend();
  resetTool();
});
window.addEventListener('resize', updateStageSize);
watch(currentAnnotationToolId, resetTool);
watch(zoom, onZoomChanged);

// When the user leave the page in your Vue app
onBeforeRouteLeave(() => {
  if (
      isEdited.value &&
      !confirm('You have unsaved changes. Are you sure you want to leave?')
  ) {
    return false;
  }
});

// When the user refresh/leave the current tab
useEventListener(window, 'beforeunload', (event) => {
  if (isEdited.value) {
    event.preventDefault();
  }
});
</script>
<style lang="scss" scoped>
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

.stage-container {
  box-shadow: 0 1px 5px rgba(0, 0, 0, 0.2), 0 2px 2px rgba(0, 0, 0, 0.14),
  0 3px 1px -2px rgba(0, 0, 0, 0.12);
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
