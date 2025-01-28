import { boot } from 'quasar/wrappers';
import axios, { AxiosInstance } from 'axios';
import {useAuthStore} from "stores/auth-store";
import {Notify} from "quasar";
import {useRouter} from "vue-router";

declare module 'vue' {
  interface ComponentCustomProperties {
    $axios: AxiosInstance;
    $api: AxiosInstance;
  }
}

export const apiPath = process.env.API_LOCATION || "/api"

// Be careful when using SSR for cross-request state pollution
// due to creating a Singleton instance here;
// If any client changes this (global) instance, it might be a
// good idea to move this instance creation inside of the
// "export default () => {}" function below (which runs individually
// for each client)
const api = axios.create({ baseURL: apiPath });

export default boot(({ app }) => {
  // for use inside Vue files (Options API) through this.$axios and this.$api

  app.config.globalProperties.$axios = axios;
  // ^ ^ ^ this will allow you to use this.$axios (for Vue Options API form)
  //       so you won't necessarily have to import axios in each vue file

  app.config.globalProperties.$api = api;
  // ^ ^ ^ this will allow you to use this.$api (for Vue Options API form)
  //       so you can easily perform requests against your app's API
});

// Request Interceptor
api.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore();
    const accessToken = authStore.accessToken;

    if (accessToken) {
      config.headers['Authorization'] = `Bearer ${accessToken}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const authStore = useAuthStore();
    const router = useRouter()
    const originalRequest = error.config;

    // Prevent infinite loop
    if(originalRequest.url == "/auth/refresh") {
      // authStore.doLogout();
      Notify.create({
        type: 'negative',
        message: 'Authentication error (account/session expired or you uploaded too many images at once).',
      });
      await router.push("/")
      return Promise.reject(error);
    }

    // Try to refresh the token
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        await authStore.doRefreshToken();
        return api(originalRequest);
      } catch (refreshError) {
        // authStore.doLogout();
        Notify.create({
          type: 'negative',
          message: 'Authentication error (account/session expired or you uploaded too many images at once)',
        });
        await router.push("/")
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export { api };
