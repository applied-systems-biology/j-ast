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

import {RouteLocationNormalized} from "vue-router";
import {useAuthStore} from "stores/auth-store";
import {UserRole} from "src/types/registration";

const beforeEach = (to: RouteLocationNormalized) => {
  if (to.path.startsWith("/account") || to.path.startsWith("/project") || to.path.startsWith("/tasks") || to.path.startsWith("/results") || to.path.startsWith("/mask-image-annotation") || to.path.startsWith("/browser")) {
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
