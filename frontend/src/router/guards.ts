import {RouteLocationNormalized} from "vue-router";
import {useAuthStore} from "stores/auth-store";
import {UserRole} from "src/types/registration";

const beforeEach = (to: RouteLocationNormalized) => {
  if (to.path.startsWith("/account") || to.path.startsWith("/project") || to.path.startsWith("/tasks") || to.path.startsWith("/results") || to.path.startsWith("/mask-image-annotation")) {
    const authStore = useAuthStore()
    if (!authStore.isLoggedIn) {
      return "/"
    }
    return true
  }
  else if(to.path.startsWith("/admin")) {
    const authStore = useAuthStore()
    if (!authStore.isLoggedIn || authStore.role != UserRole.Admin) {
      return "/"
    }
    return true
  }
  return true
}

export default beforeEach
