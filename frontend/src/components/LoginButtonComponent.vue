<template>
  <q-btn @click="showLoginDialog" v-if="!authStore.isLoggedIn" color="green">Login</q-btn>
  <q-btn @click="doLogout" v-if="authStore.isLoggedIn" color="red">Logout</q-btn>
  <q-dialog v-model="displayLoginDialog" persistent>
    <q-card>
      <q-card-section class="row items-center q-pb-none">
        <div class="text-h6">Login</div>
        <q-space/>
        <q-btn icon="close" flat round dense v-close-popup/>
      </q-card-section>

      <q-card-section>
        <q-form class="q-gutter-md" @submit="doLogin">
          <q-input type="text" v-model="loginName" filled label="Username"/>
          <q-input type="password" v-model="loginPassword" filled label="Password"/>
          <q-btn label="Login" type="submit" color="primary"/>
        </q-form>
      </q-card-section>
    </q-card>
  </q-dialog>
</template>
<script setup lang="ts">
import {ref} from "vue";
import {useQuasar} from "quasar";
import axios from "axios";
import {useAuthStore} from "stores/auth-store";

const $q = useQuasar()
const displayLoginDialog = ref(false)
const loginName = ref<string>("")
const loginPassword = ref<string>("")
const authStore = useAuthStore()

function showLoginDialog() {
  displayLoginDialog.value = true;
}

function doLogin() {

  $q.loading.show({
    message: 'Validating user credentials ...'
  })
  axios.post("/api/login", {
    username: loginName.value,
    password: loginPassword.value,
  })
    .then(response => {
      authStore.token = response.data.token
      authStore.username = response.data.username
      authStore.role = response.data.role
      authStore.authorities = response.data.authorities
      authStore.guestExpireSeconds = response.data.guestExpireSeconds
      displayLoginDialog.value = false
    })
    .catch(reason => {
      console.log(reason);
    })
    .finally(() => {
      $q.loading.hide()
    })
}

function doLogout() {
  authStore.logout()
}

</script>
<style scoped>

</style>
