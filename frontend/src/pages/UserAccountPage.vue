<template>
  <q-dialog v-model="showEditPasswordDialog" persistent>
    <q-card>
      <q-card-section class="row items-center q-pb-none">
        <div class="text-h6">Change password</div>
        <q-space />
        <q-btn icon="close" flat round dense v-close-popup />
      </q-card-section>

      <q-card-section>
        <q-form class="q-gutter-md" @submit="doEditPassword">
          <q-input
            type="password"
            v-model="currentUser.newPassword"
            filled
            label="Password"
            :rules="[
              (val) => !!val || 'Field is required',
              (val) => (val && val.length >= 6) || 'Password too short',
            ]"
            autocomplete="new-password"
          />
          <q-input
            type="password"
            v-model="currentUser.newPasswordConfirm"
            filled
            label="Confirm password"
            :rules="[
              (val) =>
                val == currentUser.newPassword || 'Passwords do not match',
            ]"
            autocomplete="off"
          />
          <q-btn label="Confirm" type="submit" color="primary" />
        </q-form>
      </q-card-section>
    </q-card>
  </q-dialog>
  <q-page padding>
    <q-card class="q-mb-lg">
      <q-card-section class="row q-gutter-sm">
        <div class="text-h6">Your account</div>
        <q-space />
        <q-btn
          label="Change password ..."
          color="primary"
          icon="key"
          @click="showEditPassword"
        />
        <q-btn
          label="Save changes"
          color="green"
          icon="save"
          @click="doEditUser"
        />
      </q-card-section>
      <q-card-section>
        <q-form
          class="q-gutter-md"
          @submit="doEditUser"
          autocorrect="off"
          autocapitalize="off"
          autocomplete="off"
          spellcheck="false"
        >
          <q-input
            type="text"
            v-model="currentUser.id"
            filled
            label="Internal ID (provide this if you have issues)"
            autocomplete="off"
            disable
          />
          <q-input
            type="text"
            v-model="currentUser.firstName"
            filled
            label="First name"
            :rules="[(val) => !!val || 'Field is required']"
            autocomplete="off"
          />
          <q-input
            type="text"
            v-model="currentUser.lastName"
            filled
            label="Last name"
            :rules="[(val) => !!val || 'Field is required']"
            autocomplete="off"
          />
          <q-input
            type="text"
            v-model="currentUser.email"
            filled
            label="E-Mail"
            disable
            autocomplete="off"
          />
          <q-input
            type="text"
            v-model="currentUser.affiliation"
            filled
            label="Affiliation"
            :rules="[(val) => !!val || 'Field is required']"
            autocomplete="off"
          />
          <q-select
            v-model="currentUser.role"
            :options="[UserRole.User, UserRole.Admin, UserRole.Guest]"
            filled
            disable
            label="Role"
          />
        </q-form>
      </q-card-section>
    </q-card>
    <q-list bordered class="rounded-borders">
      <q-expansion-item icon="key" label="Developer information">
        <q-card>
          <q-card-section>
            <div class="text-h6">JWT Authentication</div>
          </q-card-section>

          <q-card-section>
            <q-markup-table>
              <thead>
                <tr>
                  <td>Type</td>
                  <td>Token</td>
                  <td>Expires at</td>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>Access</td>
                  <td>{{ authStore.accessToken }}</td>
                  <td>{{ extractTokenExpAsString(authStore.accessToken) }}</td>
                </tr>
                <tr>
                  <td>Refresh</td>
                  <td>{{ authStore.refreshToken }}</td>
                  <td>{{ extractTokenExpAsString(authStore.refreshToken) }}</td>
                </tr>
              </tbody>
            </q-markup-table>
          </q-card-section>

          <q-separator dark />

          <q-card-actions>
            <q-btn icon="refresh" flat @click="refreshAccessToken"
              >Refresh access token
            </q-btn>
          </q-card-actions>
        </q-card>
      </q-expansion-item>
    </q-list>
  </q-page>
</template>
<script setup lang="ts">
import { extractTokenExpAsString, useAuthStore } from 'stores/auth-store';
import { UserPayload, UserRole } from 'src/types/registration';
import { onMounted, ref } from 'vue';
import { loadPayloadInstanceFromApi } from 'src/types/common';
import { api } from 'boot/axios';
import {
  sendFailureNotification,
  sendSuccessNotification,
} from 'src/types/notification';

const authStore = useAuthStore();
const currentUser = ref<UserPayload>(new UserPayload());
const showEditPasswordDialog = ref(false);

function refreshAccessToken() {
  authStore.doRefreshToken();
}

function showEditPassword() {
  currentUser.value.newPassword = '';
  currentUser.value.newPasswordConfirm = '';
  showEditPasswordDialog.value = true;
}

function doEditUser() {
  currentUser.value.newPassword = '';
  currentUser.value.newPasswordConfirm = '';
  api
    .post('current-user/update-metadata', currentUser.value)
    .then(() => {
      sendSuccessNotification('Your user metadata was changed');
    })
    .catch((error) => {
      sendFailureNotification(error + '');
    })
    .finally(() => {
      loadPayloadInstanceFromApi('current-user', UserPayload, currentUser);
    })
}

function doEditPassword() {
  showEditPasswordDialog.value = false;
  api
    .post('current-user/update-password', currentUser.value)
    .then(() => {
      sendSuccessNotification('Your password was changed.');
    })
    .catch((error) => {
      sendFailureNotification(error + '');
    })
    .finally(() => {
      loadPayloadInstanceFromApi('current-user', UserPayload, currentUser);
    })
}

onMounted(() => {
  loadPayloadInstanceFromApi('current-user', UserPayload, currentUser);
});
</script>
