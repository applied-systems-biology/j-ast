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
import { api } from 'boot/axios';

function toCacheId(imageId: number) : string {
  return `${imageId}`;
}

export const useResultItemThumbnailStore = defineStore('resultItemThumbnailStore', {
  state: () => ({
    cache: {} as Record<string, string>, // Stores image URLs by image ID
  }),
  actions: {
    fetchImage(id: number): Promise<string | undefined> {
      // Check if the image is already cached
      if (this.cache[toCacheId(id)]) {
        return Promise.resolve(this.cache[toCacheId(id)]);
      }

      // Fetch the image from the backend using .then
      return api
        .get(`/result-item/${id}/thumbnail`, { responseType: 'blob' })
        .then((response) => {
          const imageBlob = response.data;
          const objectUrl = URL.createObjectURL(imageBlob);

          // Store the image URL in the cache
          this.cache[toCacheId(id)] = objectUrl;
          return objectUrl;
        })
        .catch((error) => {
          console.error('Error fetching image:', error);
          return undefined;
        });
    },
    revokeImage(id: number) {
      if (this.cache[toCacheId(id)]) {
        URL.revokeObjectURL(this.cache[toCacheId(id)]);
        delete this.cache[toCacheId(id)];
      }
    },
  },
});
