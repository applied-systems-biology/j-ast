<template>
  <q-page padding>
    <div class="q-mb-lg">
      <q-card class="bg-blue-grey-2">
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
          <q-btn icon="refresh" flat @click="refreshAccessToken">Refresh access token</q-btn>
        </q-card-actions>
      </q-card>
    </div>
  </q-page>
</template>
<script setup lang="ts">

import {extractTokenExpAsString, useAuthStore} from "stores/auth-store";
const authStore = useAuthStore()

function refreshAccessToken() {
  authStore.doRefreshToken()
}

</script>
<style scoped>
.project-item {
  width: 12rem;
  height: 12rem;
}
</style>
