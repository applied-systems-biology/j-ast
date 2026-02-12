<template>
  <q-dialog v-model="showEditPasswordDialog" persistent>
    <q-card>
      <q-card-section class="row items-center q-pb-none">
        <div class="text-h6">Change password</div>
        <q-space/>
        <q-btn v-close-popup dense flat icon="close" round/>
      </q-card-section>

      <q-card-section>
        <q-form class="q-gutter-md" @submit="doEditPassword">
          <q-input
              v-model="currentUser.newPassword"
              :rules="[
              (val) => !!val || 'Field is required',
              (val) => (val && val.length >= 6) || 'Password too short',
            ]"
              autocomplete="new-password"
              filled
              label="Password"
              type="password"
          />
          <q-input
              v-model="currentUser.newPasswordConfirm"
              :rules="[
              (val) =>
                val == currentUser.newPassword || 'Passwords do not match',
            ]"
              autocomplete="off"
              filled
              label="Confirm password"
              type="password"
          />
          <q-btn color="primary" label="Confirm" type="submit"/>
        </q-form>
      </q-card-section>
    </q-card>
  </q-dialog>
  <q-page padding>
    <q-card class="q-mb-lg">
      <q-card-section class="row q-gutter-sm">
        <div class="text-h6">Your account</div>
        <q-space/>
        <q-btn
            color="primary"
            icon="key"
            label="Change password ..."
            no-caps
            no-wrap
            @click="showEditPassword"
        />
        <q-btn
            color="green"
            icon="save"
            label="Save changes"
            no-caps
            no-wrap
            @click="doEditUser"
        />
      </q-card-section>
      <q-card-section>
        <q-form
            autocapitalize="off"
            autocomplete="off"
            autocorrect="off"
            class="q-gutter-md"
            spellcheck="false"
            @submit="doEditUser"
        >
          <q-input
              v-model="currentUser.id"
              autocomplete="off"
              disable
              filled
              label="Internal ID (provide this if you have issues)"
              type="text"
          />
          <q-input
              v-model="currentUser.firstName"
              :rules="[(val) => !!val || 'Field is required']"
              autocomplete="off"
              filled
              label="First name"
              type="text"
          />
          <q-input
              v-model="currentUser.lastName"
              :rules="[(val) => !!val || 'Field is required']"
              autocomplete="off"
              filled
              label="Last name"
              type="text"
          />
          <q-input
              v-model="currentUser.email"
              autocomplete="off"
              disable
              filled
              label="E-Mail"
              type="text"
          />
          <q-input
              v-model="currentUser.affiliation"
              :rules="[(val) => !!val || 'Field is required']"
              autocomplete="off"
              filled
              label="Affiliation"
              type="text"
          />
          <q-select
              v-model="currentUser.role"
              :options="[UserRole.User, UserRole.Admin, UserRole.Guest]"
              disable
              filled
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

          <q-separator dark/>

          <q-card-actions>
            <q-btn flat icon="refresh" @click="refreshAccessToken"
            >Refresh access token
            </q-btn>
          </q-card-actions>
        </q-card>
      </q-expansion-item>
    </q-list>
  </q-page>
</template>
<script lang="ts" setup>
import {extractTokenExpAsString, useAuthStore} from 'stores/auth-store';
import {UserPayload, UserRole} from 'src/types/registration';
import {onMounted, ref} from 'vue';
import {loadPayloadInstanceFromApi} from 'src/types/common';
import {api} from 'boot/axios';
import {sendFailureNotification, sendSuccessNotification,} from 'src/types/notification';

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
