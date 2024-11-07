import { defineStore } from 'pinia';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: "",
    username: "",
    authorities: [],
    guestExpireSeconds: 0,
    role: ""
  }),
  getters: {
    isLoggedIn: (state) => state.token && state.token.length > 0,
  },
  actions: {
    logout() {
      this.token = ""
      this.username = ""
      this.authorities = []
      this.guestExpireSeconds = 0
      this.role = ""
    },
  },
});
