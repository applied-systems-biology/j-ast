import {RouteLocationNormalized} from "vue-router";
import {useAuthStore} from "stores/auth-store";

const beforeEach = (to: RouteLocationNormalized) => {
  if (to.path.startsWith("/account") || to.path.startsWith("/project")) {
    const authStore = useAuthStore()

    if (!authStore.isLoggedIn) {
      return "/"
    }

    return true
  }
  return true
}

export default beforeEach
