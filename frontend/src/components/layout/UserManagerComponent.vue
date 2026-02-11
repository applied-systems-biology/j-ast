<template>
  <div v-if="isWebApp()" class="q-gutter-sm flex row flex-center">
    <q-btn v-if="authStore.isGuest" color="orange-5" no-caps @click="displayGuestInfoDialog = true">
      <q-icon class="q-mr-sm" name="warning"/>
      <div class="q-mr-md">Limited account</div>
      <q-circular-progress
          :max="authStore.limits.guestMaxExpireSeconds"
          :min="0"
          :value="authStore.limits.guestExpireSeconds"
          color="red"
          reverse
          size="22px"
          track-color="white"
      >
      </q-circular-progress>
      <q-tooltip>This account will expire on {{
          formatExpirationTime(authStore.limits.guestExpireSeconds)
        }}.
      </q-tooltip>
    </q-btn>

    <q-btn
        v-if="authStore.role == 'Admin'"
        color="blue-grey"
        icon="settings"
        label="Admin"
        no-caps
        to="/admin"/>
    <q-btn
        v-if="authStore.isLoggedIn"
        color="blue-grey"
        icon="fact_check"
        label="Presets"
        no-caps
        to="/presets"
    />
    <q-btn
        v-if="authStore.isLoggedIn"
        :label="authStore.username + ' (' + authStore.role + ')'"
        color="blue-grey"
        icon="person"
        no-caps
        to="/account"
    />
    <q-btn
        v-if="!authStore.isLoggedIn"
        color="green"
        label="Register"
        @click="showRegisterDialog"/>
    <q-btn v-if="!authStore.isLoggedIn" color="green" label="Login" @click="showLoginDialog"/>
    <q-btn v-if="authStore.isLoggedIn" color="red" label="Logout" @click="doLogout"/>
  </div>

  <!-- Guest info dialog -->
  <q-dialog v-model="displayGuestInfoDialog">
    <q-card>
      <q-card-section class="row items-center q-pb-none">
        <div class="text-h6">Guest account limitations</div>
        <q-space/>
        <q-btn v-close-popup dense flat icon="close" round/>
      </q-card-section>

      <q-card-section>
        <p>You account is only a guest account and has the following limitations:</p>
        <q-list bordered separator>
          <q-item>
            <q-item-section avatar>
              <q-icon color="red" name="fa-solid fa-clock"/>
            </q-item-section>
            <q-item-section>
              <q-item-label> This account will expire on {{
                  formatExpirationTime(authStore.limits.guestExpireSeconds)
                }}
              </q-item-label>
              <q-item-label caption>You will lose all data stored in this account!</q-item-label>
            </q-item-section>
          </q-item>
          <q-item>
            <q-item-section avatar>
              <q-icon color="red" name="fa-solid fa-folder"/>
            </q-item-section>
            <q-item-section>
              <q-item-label> You can have at most
                {{ formatNumberPlural(registrationFeatures.guestProjectLimit, "project") }}
              </q-item-label>
              <q-item-label caption>J-AST will refuse to add more projects if you reached the limit</q-item-label>
            </q-item-section>
          </q-item>
          <q-item>
            <q-item-section avatar>
              <q-icon color="red" name="fa-solid fa-image"/>
            </q-item-section>
            <q-item-section>
              <q-item-label> You can have at most {{
                  formatNumberPlural(registrationFeatures.guestImageLimit, "image")
                }} per project
              </q-item-label>
              <q-item-label caption>J-AST will reject the upload of additional images</q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
      </q-card-section>
      <q-card-section>
        An administrator can upgrade your guest account into a regular account. Please contact
        {{ registrationFeatures.adminContact }}.
      </q-card-section>
    </q-card>
  </q-dialog>

  <!-- Login dialog -->
  <q-dialog v-model="displayLoginDialog" persistent>
    <q-card>
      <q-card-section class="row items-center q-pb-none">
        <div class="text-h6">Login</div>
        <q-space/>
        <q-btn v-close-popup dense flat icon="close" round/>
      </q-card-section>

      <q-card-section>
        <q-form class="q-gutter-md" @submit="doLogin">
          <q-input v-model="loginName" filled label="Username" type="text"/>
          <q-input
              v-model="loginPassword"
              filled
              label="Password"
              type="password"
          />
          <q-btn color="primary" label="Login" type="submit"/>
        </q-form>
      </q-card-section>
    </q-card>
  </q-dialog>

  <!-- Registration dialog -->
  <q-dialog v-model="displayRegisterDialog" persistent>
    <q-card class="dialog-register">
      <q-card-section class="row items-center q-pb-none">
        <div class="text-h6">Register</div>
        <q-space/>
        <q-btn v-close-popup dense flat icon="close" round/>
      </q-card-section>

      <q-card-section>
        <q-form
            autocapitalize="off"
            autocomplete="off"
            autocorrect="off"
            class="q-gutter-md"
            spellcheck="false"
            @submit="doRegister"
        >
          <q-input
              v-model="registrationData.firstName"
              :rules="[(val) => !!val || 'Field is required']"
              autocomplete="off"
              filled
              label="First name"
              type="text"
          />
          <q-input
              v-model="registrationData.lastName"
              :rules="[(val) => !!val || 'Field is required']"
              autocomplete="off"
              filled
              label="Last name"
              type="text"
          />
          <q-input
              v-model="registrationData.email"
              :rules="[
              (val) => EmailValidator.validate(val) || 'Not a valid email',
            ]"
              autocomplete="off"
              filled
              label="E-Mail"
              type="text"
          />
          <q-input
              v-model="registrationData.newPassword"
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
              v-model="registrationData.newPasswordConfirm"
              :rules="[
              (val) =>
                val == registrationData.newPassword || 'Passwords do not match',
            ]"
              autocomplete="off"
              filled
              label="Confirm password"
              type="password"
          />
          <q-input
              v-model="registrationData.affiliation"
              :rules="[(val) => !!val || 'Field is required']"
              autocomplete="off"
              filled
              label="Affiliation"
              type="text"
          />
          <q-select
              v-model="registrationData.role"
              :options="allowedRegistrationRoles"
              filled
              label="Role"
          />
          <q-banner
              v-if="registrationData.role == UserRole.Guest"
              class="text-white bg-red"
              inline-actions
              rounded
          >
            This account will be a guest account. You can only have
            {{ registrationFeatures.guestProjectLimit }}
            projects, with at most
            {{ registrationFeatures.guestImageLimit }} images per project. Your
            account will be automatically deleted after
            {{ registrationFeatures.guestAccountExpireMinutes }} minutes. If you
            want to make your account permanent and remove the restrictions,
            please contact {{ registrationFeatures.adminContact }}.
          </q-banner>
          <q-btn
              :disable="!registrationDataValid"
              color="primary"
              label="Register"
              type="submit"
          />
        </q-form>
      </q-card-section>
    </q-card>
  </q-dialog>

  <!-- Loading info for desktop app (backend server startup) -->
  <q-dialog :model-value="isDesktopApp() && !authStore.isLoggedIn" position="bottom" seamless>
    <q-card style="width: 350px">
      <q-linear-progress color="orange" indeterminate/>
      <q-card-section class="row items-center no-wrap">
        <q-spinner class="q-mr-lg" size="xl"/>
        <div>
          <div class="text-weight-bold">Loading J-AST ...</div>
          <div class="text-grey">This will take only a few seconds</div>
        </div>
      </q-card-section>
    </q-card>
  </q-dialog>
</template>
<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue';
import {useQuasar} from 'quasar';
import {useAuthStore} from 'stores/auth-store';
import {api} from 'boot/axios';
import {useRouter} from 'vue-router';
import {sendFailureNotification, sendSuccessNotification,} from 'src/types/notification';
import {UserPayload, UserRegistrationAllowedFeaturesPayload, UserRole,} from 'src/types/registration';
import {loadPayloadInstanceFromApi} from 'src/types/common';
import * as EmailValidator from 'email-validator';
import {instanceToPlain, plainToInstance} from 'class-transformer';
import {UserAuthenticationLoginResponse} from 'src/types/auth';
import {formatExpirationTime, formatNumberPlural} from 'src/types/utils';
import {isDesktopApp, isWebApp} from 'src/types/electron';
import {useIntervalFn} from '@vueuse/core';

const $q = useQuasar();
const $router = useRouter();
const displayLoginDialog = ref(false);
const displayRegisterDialog = ref(false);
const displayGuestInfoDialog = ref(false);
const loginName = ref<string>('');
const loginPassword = ref<string>('');
const authStore = useAuthStore();
const registrationFeatures = ref<UserRegistrationAllowedFeaturesPayload>(
    new UserRegistrationAllowedFeaturesPayload()
);
const registrationData = ref<UserPayload>(
    new UserPayload()
);
const registrationDataValid = computed(() => {
  if (!EmailValidator.validate(registrationData.value.email)) {
    return false;
  }
  if (
      !registrationData.value.newPassword ||
      registrationData.value.newPassword.length < 6
  ) {
    return false;
  }
  if (!registrationData.value.firstName) {
    return false;
  }
  if (!registrationData.value.lastName) {
    return false;
  }
  if (!registrationData.value.affiliation) {
    return false;
  }
  if (!registrationData.value.role) {
    return false;
  }
  return true;
});
const allowedRegistrationRoles = ref<UserRole[]>([]);

function showLoginDialog() {
  displayLoginDialog.value = true;
}

function showRegisterDialog() {
  $q.loading.show({message: 'Verifying registration features ...'});
  loadPayloadInstanceFromApi(
      '/auth/registration-features',
      UserRegistrationAllowedFeaturesPayload,
      registrationFeatures
  )
      .then((payload) => {
        if (payload.allowSelfRegister || payload.allowGuestAccounts) {
          if (!payload.allowSelfRegister && payload.allowGuestAccounts) {
            allowedRegistrationRoles.value = [UserRole.Guest];
            registrationData.value.role = UserRole.Guest;
          } else {
            allowedRegistrationRoles.value = [UserRole.Guest, UserRole.User];
            registrationData.value.role = UserRole.User;
          }
          registrationData.value.newPasswordConfirm = '';

          displayRegisterDialog.value = true;
        } else {
          sendFailureNotification(
              `Registration is currently disabled. Please contact ${registrationFeatures.value.adminContact}`
          );
        }
      })
      .finally(() => {
        $q.loading.hide();
      });
}

function doLogin() {
  $q.loading.show({
    message: 'Validating user credentials ...',
  });
  api
      .post('/auth/login', {
        username: loginName.value,
        password: loginPassword.value,
      })
      .then((response) => {
        const info = plainToInstance(UserAuthenticationLoginResponse, response.data)
        authStore.accessToken = info.accessToken;
        authStore.refreshToken = info.refreshToken;
        authStore.username = info.username;
        authStore.role = info.role;
        authStore.authorities = info.authorities;
        authStore.limits = info.toLimits()
        displayLoginDialog.value = false;
      })
      .catch((reason) => {
        console.log(reason);
      })
      .finally(() => {
        $q.loading.hide();
      });
}

function doLogout() {
  authStore.doLogout();
  $router.push('/');
  sendSuccessNotification('Successfully logged out');
}

function doRegister() {
  if (!registrationDataValid.value) {
    sendFailureNotification('The entered values are not valid!');
    return;
  }

  api
      .post('auth/register', instanceToPlain(registrationData.value))
      .then((response) => {
        sendSuccessNotification(response.data);
      })
      .catch((reason) => {
        sendFailureNotification('Unable to register: ' + reason);
      })
      .finally(() => {
        displayRegisterDialog.value = false;
        $q.loading.hide();
      });
}

function doDesktopAppLogin() {
  if (!authStore.isLoggedIn) {
    authStore.doDesktopAppLogin()
  }
}

onMounted(() => {
  loadPayloadInstanceFromApi(
      '/auth/registration-features',
      UserRegistrationAllowedFeaturesPayload,
      registrationFeatures
  )
})

if (isDesktopApp()) {
  useIntervalFn(doDesktopAppLogin, 1000)
}

</script>
<style lang="scss" scoped>
.dialog-register {
  width: 700px;
  max-width: 80vw;
}
</style>
