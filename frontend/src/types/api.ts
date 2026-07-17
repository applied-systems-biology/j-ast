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

export const apiBase: string = (() => {
  if (typeof window !== 'undefined') {

    console.log(window)

    // Check for Electron preload bridge
    const baseFromElectron = window.electronAPI?.getApiBase?.();
    if (baseFromElectron) {
      console.log("--> Obtaining API_BASE from Electron preload as " + baseFromElectron);
      return baseFromElectron;
    }

    // Through JS (directly, doesn't work always)
    const win = window as any;
    if (win.__API_BASE__) {
      console.log("--> Obtaining API_BASE from window as " + win.__API_BASE__)
      return win.__API_BASE__;
    }
  }

  if(process.env.API_LOCATION) {
    console.log("--> Obtaining API_BASE from env as " + process.env.API_LOCATION)
    return process.env.API_LOCATION;
  }

  console.log("--> Obtaining API_BASE as default /api")
  return '/api';
})();
