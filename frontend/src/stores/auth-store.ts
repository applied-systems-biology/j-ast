/*
 * Copyright (c) 2026.
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

import { defineStore } from 'pinia';
import {api} from "boot/axios";
import {jwtDecode, JwtPayload} from "jwt-decode";
import { GuestLimits } from 'src/types/auth';
import { isDesktopApp } from 'src/types/electron';

type AccessTokenField = string | number | string[] | null

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: "",
    refreshToken: "",
    username: "",
    authorities: new Array<string>(),
    limits: new GuestLimits(),
    role: "",
    desktopLogin: false
  }),
  getters: {
    isLoggedIn(): boolean {
      return Boolean(this.accessToken && this.accessToken.length > 0) || this.desktopLogin
    },
    isGuest(): boolean {
      return this.role == "Guest"
    },
    isAdmin(): boolean {
      return this.role == "Admin"
    },
    isUser(): boolean {
      return this.role == "User"
    }
  },
  actions: {
    doLogout() {
      this.accessToken = ""
      this.refreshToken = ""
      this.username = ""
      this.authorities = new Array<string>()
      this.role = ""
    },
    doDesktopAppLogin() {
      api.get('/ping').then(() => {
        this.desktopLogin = true;
      })
    },
    async doRefreshToken() {
      if(isDesktopApp()) {
        return
      }
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
        // this.doLogout();
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
