<template>
  <q-btn class="q-ml-sm" icon="help" @click="showDocumentation = true" flat/>
  <q-dialog full-height seamless :position="isLeft ? 'left' : 'right'" v-model="showDocumentation">
    <q-card class="column full-height" style="width: 20vw; max-width: 500px">
      <q-card-section class="row items-center">
        <div class="text-h6">Documentation</div>
        <q-space/>
        <q-btn v-if="!isLeft" size="sm" flat icon="fa-solid fa-chevron-left" @click="isLeft = true">
          <q-tooltip>Move to left side</q-tooltip>
        </q-btn>
        <q-btn v-if="isLeft" size="sm" flat icon="fa-solid fa-chevron-right" @click="isLeft = false">
          <q-tooltip>Move to right side</q-tooltip>
        </q-btn>
        <q-btn flat icon="close" v-close-popup/>
      </q-card-section>

      <q-card-section v-if="isWebApp()">
        <q-icon name="info"/>
        If you experience issues, please contact {{ registrationFeatures.adminContact }}
      </q-card-section>
      <q-card-section class="col-grow">
        <q-scroll-area style="width: 100%; height: 100%">
          <q-list bordered class="rounded-borders">
            <q-expansion-item
              expand-separator
              icon="help"
              label="What is J-AST?"
              caption="Basic information"
              group="documentation"
            >
              <q-card>
                <q-card-section>
                  The <strong>JIPipe Antimicrobial Susceptibility Test</strong> analysis platform is a modern web-based
                  software
                  that allows anyone to upload DDA and E-Tests and apply analyses via manual annotation and/or automated
                  algorithms.
                  <p>The tool is built on <a href="https://jipipe.org/" target="_blank">JIPipe</a>, a visual programming
                    language for ImageJ. </p>
                </q-card-section>
              </q-card>
            </q-expansion-item>
            <q-expansion-item
              expand-separator
              icon="help"
              label="Getting started"
              caption="Creating a project and uploading images"
              group="documentation"
            >
              <q-card>
                <q-card-section>
                  You first need to login (if using the web application). If you do not have an account yet,
                  you might need to create one using the <strong>Register</strong> button.
                </q-card-section>
                <q-card-section class="bg-green-1">
                  <q-icon name="info"/>
                  If you can create normal/guest or no account depends on the web server settings. Please contact {{ registrationFeatures.adminContact }}
                  if no registration option is available or if you want to turn a guest account into a permanent account.
                </q-card-section>
                <q-card-section>
                  You will be presented with the option to create a new <strong>Project</strong> and the ability to set the project name.
                  On opening a project, you will see an <strong>Upload</strong> button:
                  <q-btn class="d-block q-ma-md" color="green" label="Upload" icon="upload"/>
                  Click the button and press the <q-icon name="add_box" size="md"/> button to add images into the queue.
                </q-card-section>
                <q-card-section class="bg-amber-1">
                  <q-icon name="info"/>
                  We highly recommend to not add too many images at once (at most 20-30), as your browser will slow down and
                  might lose connection to the server.
                </q-card-section>
                <q-card-section>
                  Then press the <strong>Upload now</strong> button to start the upload:
                  <q-btn class="d-block q-ma-md" color="green" label="Upload now" icon="upload"/>
                  The uploaded images then will be placed into the <strong>Unsorted images</strong> drawer.
                </q-card-section>
              </q-card>
            </q-expansion-item>
            <q-expansion-item
              expand-separator
              icon="info"
              label="Preprocessing images"
              caption="Important information"
              group="documentation"
            >
              <q-card>
                <q-card-section>
                  J-AST assumes that images have a <strong>black background with bright colonies</strong>. If this is not the case,
                  please follow this guide:
                </q-card-section>
                <q-card-section>
                  <ol>
                    <li>Click <q-btn class="q-ma-md" color="secondary" label="Select all unsorted" icon="select_all"/></li>
                    <li>Click <q-btn class="q-ma-md"  color="accent"
                                     label="Process"
                                     icon="fa-solid fa-gear"/></li>
                    <li>Navigate to <strong>Preprocessing | Invert raw image</strong></li>
                    <li>Scroll down in the dialog and click <q-btn label="OK" color="red"/> </li>
                    <li>Wait a few minutes/seconds until your images are processed</li>
                  </ol>
                </q-card-section>
              </q-card>
            </q-expansion-item>
            <q-expansion-item
              expand-separator
              icon="fa-solid fa-magic-wand-sparkles"
              label="All-in-one preparation"
              caption="Organizing your project for the analysis automatically"
              group="documentation"
            >
              <q-card>
                <q-card-section>
                  J-AST comes with an automated operation that attempts to automatically annotate and sort your
                  images based on a few rules. The tool will also run a few automated detections (plate, DDA disk, E-test strip, E-test ZOI shape, E-test ZOI shape registration)
                  so the images are ready for further analysis steps.
                </q-card-section>
                <q-card-section class="bg-amber-1">
                  <q-icon name="info"/>
                  If the operations fail, you can still do the preparation step-by-step (see other categories below).
                  You can also reset all assignments made by this operation by selecting the image(s) and using the
                  operations in the <q-btn class="q-ma-md"  color="accent" label="Process" icon="fa-solid fa-gear"/> menu.
                </q-card-section>
                <q-card-section>
                  Assuming you start with unsorted images, you can execute the operation by clicking the following button:
                  <q-btn class="d-block q-ma-md" color="green" label="All-in-one preparation" icon="fa-solid fa-magic-wand-sparkles"/>
                  Please review the parameters carefully and start the operation by clicking <q-btn label="OK" color="red"/>.
                </q-card-section>
              </q-card>
            </q-expansion-item>
            <q-expansion-item
              expand-separator
              icon="help"
              label="Adding metadata"
              caption="Setting basic information about your images"
              group="documentation"
            >
              <q-card>
                <q-card-section>
                  J-AST organizes images into <strong>assay type</strong>, <strong>experiment</strong>, <strong>sample</strong>, and
                  <strong>time point</strong>. These annotations are absolutely essential for all other processes provided by J-AST.
                </q-card-section>
                <q-card-section>
                  <strong>Annotating manually: </strong> Just select the image and edit the fields on the right-hand side.
                </q-card-section>
                <q-card-section>
                  <strong>Annotating automatically: </strong> Select the images to annotate and click
                  <q-btn class="q-ma-md"  color="accent" label="Process" icon="fa-solid fa-gear"/>. <br/>Here you select the <strong>Auto-fill metadata</strong> operation.
                </q-card-section>
              </q-card>
            </q-expansion-item>
            <q-expansion-item
              expand-separator
              icon="help"
              label="Sorting images"
              caption="Creating time series"
              group="documentation"
            >
              <q-card>
                <q-card-section>
                  J-AST was designed to work on time series that are either created manually and/or by an automated sorting algorithm.
                  To ensure that a project is ready for analyses, you have to move images from the <strong>Unsorted</strong> drawer into the
                  grid below.
                </q-card-section>
                <q-card-section>
                  <strong>Sorting manually: </strong> Just drag and drop the images within the grid.
                </q-card-section>
                <q-card-section>
                  <strong>Annotating automatically: </strong> Select the images to annotate and click
                  <q-btn class="q-ma-md"  color="accent" label="Process" icon="fa-solid fa-gear"/>. <br/>Here you select the <strong>Auto-sort by metadata </strong> operation.
                </q-card-section>
              </q-card>
            </q-expansion-item>
            <q-expansion-item
              expand-separator
              icon="help"
              label="Annotating regions of interest"
              caption="Plate/disk/strip/ZOI"
              group="documentation"
            >
              <q-card>
                <q-card-section>
                  Depending on the assay type, each image supports a variety of mask-based annotations, i.e. ones that highlight specific regions of interest.
                  The following annotations are supported:
                  <ul>
                    <li><strong>Plate: </strong> the plate area</li>
                    <li><strong>DDA disk: </strong> the disk in the center of disk diffusion assays</li>
                    <li><strong>E-Test strip: </strong> the strip in E-tests</li>
                    <li><strong>E-Test ZOI shape: </strong> the Zone Of Inhibition of E-Tests. <i>Only required for the first time point, as you can register
                    the ZOI from the first to the other time points.</i></li>
                  </ul>
                  J-AST provides both algorithms that attempt to detect the above areas automatically, and capabilities to either create such ROI manually or correct
                  automatically generated results.
                </q-card-section>
                <q-card-section>
                  <strong>Annotating manually: </strong> Select the image of interest and find the annotation in the right-hand panel. Click the <strong>Edit</strong>
                  button to open the image editor. Do not forget to save the annotation when finished.
                </q-card-section>
                <q-card-section>
                  <strong>Annotating automatically: </strong> Select the images to annotate and click
                  <q-btn class="q-ma-md"  color="accent" label="Process" icon="fa-solid fa-gear"/>. <br/>Browse through the list of algorithms and select the one you require.
                </q-card-section>
                <q-card-section>
                  <strong>Registering ZOI shapes: </strong> If you have the ZOI shape of an E-test for the first time point, you can use an automated algorithm to
                  register the ZOI shape to the other time points. The algorithm will account for slight variances between the images.
                  Select the images to annotate and click
                  <q-btn class="q-ma-md"  color="accent" label="Process" icon="fa-solid fa-gear"/>. Then navigate to <strong>E-Test | Copy and register ZOI shape across timeline</strong>.
                </q-card-section>
              </q-card>
            </q-expansion-item>
            <q-expansion-item
              expand-separator
              icon="help"
              label="Analyzing images"
              caption="Creating results and visualizations"
              group="documentation"
            >
              <q-card>
                <q-card-section>
                  Select the images to analyze and click the <q-btn class="q-ma-md"  color="accent" label="Process" icon="fa-solid fa-gear"/>.
                  Here you can find operations in the <strong>Visualize</strong> and <strong>Analyze</strong> categories.
                </q-card-section>
                <q-card-section>
                  Results generated by such operations are stored in a dedicated <strong>Results</strong> area that can be accessed by clicking the
                  <q-btn label="Results" color="cyan" icon="archive"/> button.
                  The results browser allows you to review the results and download them.
                </q-card-section>
              </q-card>
            </q-expansion-item>
            <q-expansion-item
              expand-separator
              icon="help"
              label="Managing tasks"
              caption="Reviewing/cancelling tasks"
              group="documentation"
            >
              <q-card>
                <q-card-section>
                  On starting a longer process via the <strong>Process</strong> menu, J-AST will lock the whole project and display information about the currently running task at the bottom of the window.
                </q-card-section>
                <q-card-section>
                  To review past tasks or cancel the current task, click the <q-btn label="All Tasks Finished" icon="check" color="cyan"/> / <q-btn label="x tasks are running" icon="fa-solid fa-circle-notch" color="cyan"/> button and select
                  the task in the list. Here you can download the log or cancel an existing task.
                </q-card-section>
              </q-card>
            </q-expansion-item>
          </q-list>
        </q-scroll-area>
      </q-card-section>
    </q-card>
  </q-dialog>
</template>
<script setup lang="ts">
import {onMounted, ref} from "vue";
import {UserRegistrationAllowedFeaturesPayload} from "src/types/registration";
import {loadPayloadInstanceFromApi} from "src/types/common";
import { isWebApp } from 'src/types/electron';

const showDocumentation = ref(false)
const registrationFeatures = ref<UserRegistrationAllowedFeaturesPayload>(
  new UserRegistrationAllowedFeaturesPayload()
);
const isLeft = ref(false);

onMounted(() => {
  loadPayloadInstanceFromApi(
    '/auth/registration-features',
    UserRegistrationAllowedFeaturesPayload,
    registrationFeatures
  )
})

</script>
<style scoped lang="scss">

</style>
