import { defineStore } from 'pinia';
import {api} from "boot/axios";
import {jwtDecode, JwtPayload} from "jwt-decode";

type AccessTokenField = string | number | string[] | null

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: "",
    refreshToken: "",
    username: "",
    authorities: [],
    guestExpireSeconds: 0,
    role: ""
  }),
  getters: {
    isLoggedIn(): boolean {
      return Boolean(this.accessToken && this.accessToken.length > 0)
    },
  },
  actions: {
    doLogout() {
      this.accessToken = ""
      this.refreshToken = ""
      this.username = ""
      this.authorities = []
      this.guestExpireSeconds = 0
      this.role = ""
    },
    async doRefreshToken() {
      if(!this.isLoggedIn) {
        throw Error("Not logged in");
      }
      try {
        const response = await api.post('/auth/refresh', {
          refreshToken: this.refreshToken,
          accessToken: this.accessToken
        });
        this.accessToken = response.data.accessToken;
        this.refreshToken = response.data.refreshToken
      } catch (error) {
        this.doLogout();
        throw error;
      }
    }
  },
});

export const extractTokenField = function (token : string, field : string) : AccessTokenField {
  if(token) {
    try {
      const decoded = jwtDecode<JwtPayload>(token)
      const value = decoded[field as keyof JwtPayload]
      if(!value) {
        return null
      }
      return value
    }
    catch (error) {
      console.error("Error decoding access token:", error)
    }
  }
  return null
}

export const extractTokenExpAsString = function(token: string) : string {
  const value = extractTokenField(token, "exp")
  if (typeof value === "number") {
    return new Date(Number(value) * 1000).toLocaleString()
  }
  return "Unknown"
}
