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
  <q-btn no-caps no-wrap align="left" class="w-100" color="primary" icon="upload"
         label="Upload images" @click="upload()"/>
  <q-btn no-caps no-wrap align="left" class="w-100" color="primary" icon="select_all"
         label="Select all" @click="selectAll()"/>
  <q-separator />
  <q-btn no-caps no-wrap align="left" class="w-100" color="primary" icon="select_all"
         label="Select all DDAs" @click="selectAllOfAssayType(AssayType.DDA)"/>
  <q-btn no-caps no-wrap align="left" class="w-100" color="primary" icon="select_all"
         label="Select all E-Tests" @click="selectAllOfAssayType(AssayType.ETest)"/>
  <q-btn no-caps no-wrap align="left" class="w-100" color="primary" icon="select_all"
         label="Select all without type" @click="selectAllOfAssayType(AssayType.Unknown)"/>
  <q-separator />
  <q-btn no-caps no-wrap align="left" class="w-100" color="primary" icon="fa-solid fa-tag"
         label="Select all without plate" @click="selectAllWithoutMaskAnnotation('plate')"/>
  <q-btn no-caps no-wrap align="left" class="w-100" color="primary" icon="fa-solid fa-ruler"
         label="Select all without calibration" @click="selectAllWithoutCalibration()"/>
  <q-btn no-caps no-wrap align="left" class="w-100" color="primary" icon="fa-solid fa-tag"
         label="Select all without plate" @click="selectAllWithoutDDADisk()"/>
  <q-btn no-caps no-wrap align="left" class="w-100" color="primary" icon="fa-solid fa-tag"
         label="Select all without strip" @click="selectAllWithoutETestStrip()"/>
  <q-btn no-caps no-wrap align="left" class="w-100" color="primary" icon="fa-solid fa-tag"
         label="Select all without ZOI shape" @click="selectAllWithoutMaskAnnotation('zoi-shape')"/>
  <q-btn no-caps no-wrap align="left" class="w-100" color="primary" icon="fa-solid fa-ruler-vertical"
         label="Select all without strip preset" @click="selectAllWithoutStripPreset()"/>
</template>
<script lang="ts" setup>
import {ProjectImagesPayload} from "src/types/projectImages";
import {AssayType} from "src/types/assayType";
import {imageHasMaskAnnotation, ImagePayload, imageSupportsMaskAnnotation} from "src/types/image";
import {plainToInstance} from "class-transformer";

const selectedImageIds = defineModel<Array<number>>("selectedImageIds", {required: true})
const projectImages = defineModel<ProjectImagesPayload>("projectImages", {required: true})
const uploaderToggle = defineModel<boolean>("uploaderToggle", {required: true})

function upload() {
  uploaderToggle.value = true
}

function selectAll() {
  selectedImageIds.value = [...projectImages.value.imageIds]
}

function selectAllOfAssayType(assayType : AssayType) {
  selectedImageIds.value = projectImages.value.getAllImages().filter((item) => item.assayType == assayType).map(item => item.id)
}

function selectAllWithoutMaskAnnotation(annotationTypeName : string) {
  selectedImageIds.value = projectImages.value.getAllImages().filter((item) => imageSupportsMaskAnnotation(item, annotationTypeName) && !imageHasMaskAnnotation(item, annotationTypeName)).map(item => item.id)
}

function selectAllWithoutDDADisk() {
  selectedImageIds.value = projectImages.value.getAllImages().filter((item) => item.assayType == AssayType.DDA && !imageHasMaskAnnotation(item, "strip-disk")).map(item => item.id)
}

function selectAllWithoutETestStrip() {
  selectedImageIds.value = projectImages.value.getAllImages().filter((item) => item.assayType == AssayType.ETest && !imageHasMaskAnnotation(item, "strip-disk")).map(item => item.id)
}


function selectAllWithoutCalibration() {
  selectedImageIds.value = projectImages.value.getAllImages().filter((item) => item.pixelSizeMillimeter <= 0).map(item => item.id)
}

function selectAllWithoutStripPreset() {
  selectedImageIds.value = projectImages.value.getAllImages().filter((item) => item.assayType == AssayType.ETest && !plainToInstance(ImagePayload, item).hasMetadata("stripPreset")).map(item => item.id)
}


</script>
<style lang="scss" scoped>

</style>
