import { defineStore } from 'pinia';
import { api } from 'boot/axios';

export const useProjectImageThumbnailStore = defineStore('projectImageThumbnailStore', {
  state: () => ({
    cache: {} as Record<string, string>, // Stores image URLs by image ID
  }),
  actions: {
    fetchImage(id: number): Promise<string | undefined> {
      // Check if the image is already cached
      if (this.cache[id]) {
        return Promise.resolve(this.cache[id.toString()]);
      }

      // Fetch the image from the backend using .then
      return api
        .get(`/image/${id}/thumbnail`, { responseType: 'blob' })
        .then((response) => {
          const imageBlob = response.data;
          const objectUrl = URL.createObjectURL(imageBlob);

          // Store the image URL in the cache
          this.cache[id.toString()] = objectUrl;
          return objectUrl;
        })
        .catch((error) => {
          console.error('Error fetching image:', error);
          return undefined;
        });
    },
    revokeImage(id: number) {
      if (this.cache[id.toString()]) {
        URL.revokeObjectURL(this.cache[id.toString()]);
        delete this.cache[id.toString()];
      }
    },
  },
});
