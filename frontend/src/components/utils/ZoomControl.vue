<template>
  <div class="row items-center no-wrap shadow-2 bg-blue rounded-borders" style="min-height: 2.572em">
    <q-btn dense flat icon="remove" @click="decreaseZoom"/>
    <q-btn class="q-mx-sm" dense flat style="min-width: 60px;">
      {{ Math.round(model! * 100) }}%
      <q-menu auto-close>
        <q-list style="min-width: 100px">
          <q-item
              v-for="option in zoomOptions"
              :key="option"
              clickable
              @click="selectZoom(option / 100)"
          >
            <q-item-section>{{ option }}%</q-item-section>
          </q-item>
        </q-list>
      </q-menu>
    </q-btn>

    <q-btn dense flat icon="add" @click="increaseZoom"/>
  </div>
</template>

<script lang="ts" setup>
import {defineModel} from 'vue'

const model = defineModel<number>({required: true})

// Preset display options (percent format)
const zoomOptions = [25, 50, 75, 100, 125, 150, 200, 250, 300]

// Zoom behavior parameters (normalized scale)
const ZOOM_MIN = 0.25
const ZOOM_MAX = 3.0
const ZOOM_FACTOR = 1.25

function selectZoom(val: number) {
  if (val >= ZOOM_MIN && val <= ZOOM_MAX) {
    model.value = val
  }
}

function increaseZoom() {
  const current = model.value ?? 1
  const next = Math.min(ZOOM_MAX, roundToTwo(current * ZOOM_FACTOR))
  model.value = next
}

function decreaseZoom() {
  const current = model.value ?? 1
  const prev = Math.max(ZOOM_MIN, roundToTwo(current / ZOOM_FACTOR))
  model.value = prev
}

function roundToTwo(val: number): number {
  return Math.round(val * 100) / 100
}
</script>
