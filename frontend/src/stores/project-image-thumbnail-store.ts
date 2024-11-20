import { defineStore } from 'pinia';
import { api } from 'boot/axios';

function toCacheId(imageId: number, version: number) : string {
  return `${imageId}_${version}`;
}

export const useProjectImageThumbnailStore = defineStore('projectImageThumbnailStore', {
  state: () => ({
    cache: {} as Record<string, string>, // Stores image URLs by image ID
  }),
  actions: {
    fetchImage(id: number, version: number): Promise<string | undefined> {
      // Check if the image is already cached
      if (this.cache[toCacheId(id, version)]) {
        return Promise.resolve(this.cache[toCacheId(id, version)]);
      }

      // Fetch the image from the backend using .then
      return api
        .get(`/image/${id}/thumbnail`, { responseType: 'blob' })
        .then((response) => {
          const imageBlob = response.data;
          const objectUrl = URL.createObjectURL(imageBlob);

          // Store the image URL in the cache
          this.cache[toCacheId(id, version)] = objectUrl;
          return objectUrl;
        })
        .catch((error) => {
          console.error('Error fetching image:', error);
          return undefined;
        });
    },
    revokeImage(id: number, version: number) {
      if (this.cache[toCacheId(id, version)]) {
        URL.revokeObjectURL(this.cache[toCacheId(id, version)]);
        delete this.cache[toCacheId(id, version)];
      }
    },
  },
});
