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

function toCacheId(imageId: number, annotationTypeId: string, version: number) : string {
  return `${imageId}_${annotationTypeId}_${version}`;
}

export const useMaskImageAnnotationThumbnailStore = defineStore('maskImageAnnotationThumbnailStore', {
  state: () => ({
    cache: {} as Record<string, string>, // Stores image URLs by id_version
  }),
  actions: {
    fetchImage(imageId: number, annotationTypeId: string, version: number): Promise<string | undefined> {
      // Check if the image is already cached
      if (this.cache[toCacheId(imageId, annotationTypeId, version)]) {
        return Promise.resolve(this.cache[toCacheId(imageId, annotationTypeId, version)]);
      }

      // Fetch the image from the backend using .then
      return api
        .get(`/mask-image-annotation/${imageId}/${annotationTypeId}/thumbnail`, { responseType: 'blob' })
        .then((response) => {
          const imageBlob = response.data;
          const objectUrl = URL.createObjectURL(imageBlob);

          // Store the image URL in the cache
          this.cache[toCacheId(imageId, annotationTypeId, version)] = objectUrl;
          return objectUrl;
        })
        .catch((error) => {
          console.error('Error fetching image:', error);
          return undefined;
        });
    },
    revokeImage(imageId: number, annotationTypeId: string, version: number) {
      const cacheId = toCacheId(imageId, annotationTypeId, version)
      if (this.cache[cacheId]) {
        URL.revokeObjectURL(this.cache[cacheId]);
        delete this.cache[cacheId];
      }
    },
  },
});
