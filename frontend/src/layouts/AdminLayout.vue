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
  <q-layout view="hHh lpR fFf">
    <q-header>
      <q-toolbar>
        <q-toolbar-title class="row items-center q-gutter-sm">
          <HeaderLogoButtonComponent/>
        </q-toolbar-title>
        <UserManagerComponent/>
        <DocumentationComponent/>
      </q-toolbar>
    </q-header>
    <q-drawer
      side="left"
      :model-value="true"
      elevated
      class="q-pa-sm q-gutter-sm"
    >
      <div class="flex q-mb-lg">
        <q-btn no-caps no-wrap color="green" icon="add" @click="showAddUserDialog">Add new user</q-btn>
        <div class="col-grow" />
        <q-btn no-caps no-wrap color="primary" icon="refresh" @click="queryBackend"/>
      </div>
      <div class="flex column">
        <q-btn no-caps no-wrap class="user-button" @click="showUserPage(new UserPayload())" align="left" :color="!currentlyDisplayedUserData.email ? 'primary' : 'blue-grey-3'">
          <div class="text-left">
            <div><q-icon name="person"/> <i>Administrator</i></div>
            <div class="text-weight-regular"><q-icon name=""/> Administrator</div>
          </div>
        </q-btn>
        <template v-for="user in userList" :key="user.id">
          <q-btn no-caps no-wrap class="user-button" @click="showUserPage(user)" align="left" :color="user.email == currentlyDisplayedUserData.email ? 'primary' : 'blue-grey-3'">
            <div class="text-left">
              <div><q-icon name="person"/> {{ user.email }}</div>
              <div class="text-weight-regular"><q-icon name=""/> {{ user.role }}{{ !user.allowLogin ? " (Inactive)" : "" }}</div>
            </div>
          </q-btn>
        </template>
      </div>
    </q-drawer>
    <q-page-container>
      <q-page padding>
        <q-toolbar class="bg-primary text-white rounded-borders q-mb-lg">
          <q-btn no-caps no-wrap :disable="!currentlyDisplayedUserData.email" color="green" icon="edit" @click="showEditUserDialog(currentlyDisplayedUserData)">Edit {{currentlyDisplayedUserData.email}}</q-btn>
        </q-toolbar>
        <div class="flex column">
          <q-btn no-caps no-wrap v-for="project in projectList" :key="project.id" align="left" class="q-mb-sm" :to="`/project/${project.id}`">
            <div class="text-left">
              <div><q-icon name="folder"/> {{ project.name }}</div>
              <div class="text-weight-regular"><q-icon name=""/> ID: {{ project.id }}</div>
            </div>
          </q-btn>
        </div>
      </q-page>
    </q-page-container>
  </q-layout>
  <!-- Add/edit user dialog -->
  <q-dialog v-model="displayAddEditUserDialog" persistent>
    <q-card class="dialog-add-edit-user">
      <q-card-section class="row items-center q-pb-none">
        <div class="text-h6">{{ addEditUserDialogTitle }}</div>
        <q-space/>
        <q-btn icon="close" flat round dense v-close-popup/>
      </q-card-section>
      <q-card-section>
        <q-form
          class="q-gutter-md"
          @submit="doAddEditUser"
          autocorrect="off"
          autocapitalize="off"
          autocomplete="off"
          spellcheck="false"
        >
          <q-input
            type="text"
            v-model="currentlyEditedUserData.firstName"
            filled
            label="First name"
            :rules="[(val) => !!val || 'Field is required']"
            autocomplete="off"
          />
          <q-input
            type="text"
            v-model="currentlyEditedUserData.lastName"
            filled
            label="Last name"
            :rules="[(val) => !!val || 'Field is required']"
            autocomplete="off"
          />
          <q-input
            type="text"
            v-model="currentlyEditedUserData.email"
            filled
            label="E-Mail"
            :rules="[
              (val) => EmailValidator.validate(val) || 'Not a valid email',
            ]"
            :disable="addEditUserDialogEditMode"
            autocomplete="off"
          />
          <q-input
            type="password"
            v-model="currentlyEditedUserData.newPassword"
            filled
            :label="addEditUserDialogEditMode ? 'Password (leave empty to keep unchanged)' : 'Password'"
            :rules="[
              (val) => (!!val || addEditUserDialogEditMode) || 'Field is required',
              (val) => ((val && val.length >= 6) || addEditUserDialogEditMode) || 'Password too short',
            ]"
            autocomplete="new-password"
          />
          <q-input
            type="password"
            v-model="currentlyEditedUserData.newPasswordConfirm"
            filled
            label="Confirm password"
            :rules="[
              (val) =>
                (val == currentlyEditedUserData.newPassword || addEditUserDialogEditMode) || 'Passwords do not match',
            ]"
            autocomplete="off"
          />
          <q-input
            type="text"
            v-model="currentlyEditedUserData.affiliation"
            filled
            label="Affiliation"
            :rules="[(val) => !!val || 'Field is required']"
            autocomplete="off"
          />
          <q-select
            v-model="currentlyEditedUserData.role"
            :options="[UserRole.User, UserRole.Admin, UserRole.Guest]"
            filled
            label="Role"
          />
          <div>
            <q-checkbox
              v-model="currentlyEditedUserData.allowLogin"
              label="Allow Login" />
          </div>
          <q-btn
            :label="addEditUserDialogAction"
            type="submit"
            color="primary"
            :disable="!registrationDataValid"
          />
        </q-form>
      </q-card-section>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import HeaderLogoButtonComponent from "components/layout/HeaderLogoButtonComponent.vue";
import UserManagerComponent from "components/layout/UserManagerComponent.vue";
import * as EmailValidator from "email-validator";
import {UserPayload, UserRole} from "src/types/registration";
import {computed, onMounted, ref} from "vue";
import {loadPayloadInstanceFromApi} from "src/types/common";
import {sendFailureNotification, sendSuccessNotification} from "src/types/notification";
import {api} from "boot/axios";
import {instanceToPlain} from "class-transformer";
import {ProjectMetadataPayload} from "src/types/project";
import DocumentationComponent from "components/layout/DocumentationComponent.vue";

const userList = ref<UserPayload[]>([])
const projectList = ref<ProjectMetadataPayload[]>([])
const addEditUserDialogEditMode = ref<boolean>(false)
const addEditUserDialogAction = ref("Add")
const addEditUserDialogTitle = ref("Add user")
const displayAddEditUserDialog = ref(false)
const currentlyEditedUserData = ref<UserPayload>(
  new UserPayload()
);
const currentlyDisplayedUserData = ref<UserPayload>(new UserPayload())
const registrationDataValid = computed(() => {
  if (!EmailValidator.validate(currentlyEditedUserData.value.email)) {
    return false;
  }
  if (
    !currentlyEditedUserData.value.newPassword ||
    currentlyEditedUserData.value.newPassword.length < 6
  ) {
    return addEditUserDialogEditMode.value;
  }
  if (!currentlyEditedUserData.value.firstName) {
    return false;
  }
  if (!currentlyEditedUserData.value.lastName) {
    return false;
  }
  if (!currentlyEditedUserData.value.affiliation) {
    return false;
  }
  if (!currentlyEditedUserData.value.role) {
    return false;
  }
  return true;
});

function doAddEditUser() {
  if (addEditUserDialogEditMode.value) {
    if (!registrationDataValid.value) {
      sendFailureNotification('The entered values are not valid!');
      return;
    }

    api
      .post('admin/edit-user', instanceToPlain(currentlyEditedUserData.value))
      .then((response) => {
        sendSuccessNotification(response.data);
      })
      .catch((reason) => {
        sendFailureNotification('Unable to edit: ' + reason);
      })
      .finally(() => {
        displayAddEditUserDialog.value = false;
        queryBackend()
      });

  } else {
    if (!registrationDataValid.value) {
      sendFailureNotification('The entered values are not valid!');
      return;
    }

    api
      .post('auth/register', instanceToPlain(currentlyEditedUserData.value))
      .then((response) => {
        sendSuccessNotification(response.data);
      })
      .catch((reason) => {
        sendFailureNotification('Unable to register: ' + reason);
      })
      .finally(() => {
        displayAddEditUserDialog.value = false;
        queryBackend()
      });
  }
}

function showAddUserDialog() {
  addEditUserDialogTitle.value = "Add new user";
  currentlyEditedUserData.value = new UserPayload()
  addEditUserDialogAction.value = "Create"
  displayAddEditUserDialog.value = true;
  addEditUserDialogEditMode.value = false
}

function showEditUserDialog(user: UserPayload) {
  addEditUserDialogTitle.value = `Edit user ${user.email}`
  currentlyEditedUserData.value = user;
  addEditUserDialogAction.value = "Edit"
  displayAddEditUserDialog.value = true;
  addEditUserDialogEditMode.value = true
  currentlyEditedUserData.value.newPasswordConfirm = "";
}

function showUserPage(user: UserPayload) {
  currentlyDisplayedUserData.value = user;
  queryBackend()
}

function queryBackend() {
  loadPayloadInstanceFromApi("/admin/list-users", UserPayload, userList).catch(err => console.log(err));
  loadPayloadInstanceFromApi("/admin/list-projects/" + currentlyDisplayedUserData.value.id, ProjectMetadataPayload, projectList)
}

onMounted(() => {
  queryBackend()
})

</script>
<style scoped lang="scss">
.dialog-add-edit-user {
  width: 700px;
  max-width: 80vw;
}

.user-button {
  flex-grow: 1;
  width: 100%;
  height: 100%;
  overflow: hidden;
  margin-bottom: 4px;
}
</style>
