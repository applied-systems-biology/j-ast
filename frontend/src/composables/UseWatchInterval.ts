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

import {ref, watch, onBeforeUnmount, Ref} from 'vue';
type Callback = () => void;

export function useWatchInterval(source : Ref<boolean>, callback: Callback, interval = 60000) {
  const intervalId = ref<ReturnType<typeof setInterval> | null>(null);

  const startInterval = () => {
    intervalId.value = setInterval(callback, interval);
  };

  const stopInterval = () => {
    if (intervalId.value !== null) {
      clearInterval(intervalId.value);
      intervalId.value = null;
    }
  };

  watch(source, (newValue) => {
    if (newValue) {
      startInterval();
      callback(); // Call immediately when the source changes
    } else {
      stopInterval();
    }
  }, {immediate: true});

  onBeforeUnmount(stopInterval);
}
