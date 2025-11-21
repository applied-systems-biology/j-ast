import { ZipItem } from 'src/types/zip';
import { ensureExtension } from 'src/types/common';
import { plainToInstance } from 'class-transformer';
import { ImagePayload } from 'src/types/image';
import { api } from 'boot/axios';
import JSZip from 'jszip';

/**
 * Collects all necessary ZIP items for a project archive
 * @param items the image items
 */
export function collectProjectArchiveContents(items: ImagePayload[]) {
  const zipItems: Array<ZipItem> = []
  let downloadSizeBytes = 0
  const metadata: Record<string, any> = {}
  for (const item of items) {
    const fileName = "" + item.id
    zipItems.push({ entryName: ensureExtension(fileName), url: `/image/${item.id}/raw`, content: null })
    downloadSizeBytes += item.size

    // Add annotations
    for (const annotation of item.maskImageAnnotations) {
      const annotationFileName = annotation.annotationTypeId + "/" + fileName
      zipItems.push({ entryName: ensureExtension(annotationFileName), url: `/mask-image-annotation/${item.id}/${annotation.annotationTypeId}/raw`, content: null })
      downloadSizeBytes += annotation.size
    }

    // Add metadata
    metadata["" + item.id] = plainToInstance(ImagePayload, item).getMetadataAsDict()
  }

  // Create ZIP entry for metadata
  zipItems.push({ entryName: "metadata.json", url: null, content: JSON.stringify(metadata, null, 4) })
  return { zipItems, downloadSizeBytes };
}

export function uploadProjectArchive(projectId: number,
                                     projectArchiveFile : File,
                                     onProgress: (progress: number, info: string) => void,
                                     onCancel: () => boolean) : Promise<void> {
  const zip = new JSZip();
  onProgress(0, "Loading ZIP files ...")
  if(onCancel()) {
    return Promise.reject("Cancelled")
  }
  return zip.loadAsync(projectArchiveFile).then((zip) => {
    if(onCancel()) {
      return Promise.reject("Cancelled")
    }

    // TODO 1: Get the metadata.json from the ZIP and read it in. It's a Record<string, ImagePayload>
    const metadataEntry = zip.file("metadata.json");
    if (!metadataEntry) {
      throw new Error("metadata.json not found in ZIP archive");
    }

    return metadataEntry.async("text").then((metadataText) => {
      const metadata: Record<string, ImagePayload> = JSON.parse(metadataText);
      const totalItems = Object.keys(metadata).length;
      let processedItems = 0;

      // Track mapping between original keys and new image IDs
      const idMapping: Record<string, number> = {};

      // For each key in the metadata do the following: Get the image data from ZIP (entry is known: /[key].png within the ZIP). Then we upload each image to /project/${projectId}/upload-raw-image
      const uploadPromises: Promise<void>[] = [];

      for (const key of Object.keys(metadata)) {
        if (onCancel()) {
          return Promise.reject("Cancelled");
        }

        const imageEntry = zip.file(`${key}.png`);
        if (!imageEntry) {
          console.warn(`Image ${key}.png not found in ZIP, skipping`);
          processedItems++;
          onProgress((processedItems / totalItems) * 100, `Processing ${processedItems}/${totalItems} items...`);
          continue;
        }

        uploadPromises.push(
          imageEntry.async("blob").then((imageBlob) => {
            console.log(`Uploading raw image ${key}.png ...`);
            const formData = new FormData();
            formData.append("file", imageBlob, `${key}.png`);

            return api.post(`/project/${projectId}/upload-raw-image`, formData, {
              headers: {
                'Content-Type': 'multipart/form-data'
              }
            }).then((response) => {
              const imagePayload = response.data as ImagePayload;
              // Should return one image payload
              if (imagePayload) {
                console.log(`Uploading raw image ${key}.png ... Found ID mapping ${key}=${imagePayload.id}`);
                idMapping[key] = imagePayload.id;
              }
              processedItems++;
              onProgress((processedItems / totalItems) * 100, `Uploaded ${processedItems}/${totalItems} images...`);
            });
          })
        );
      }

      return Promise.all(uploadPromises).then(() => {
        // Now as we now the image IDs, post for each image the /image/{id}/update with the updated ImagePayload where metadata was taken from the Record
        const updatePromises: Promise<void>[] = [];

        for (const [key, imageId] of Object.entries(idMapping)) {
          if (onCancel()) {
            return Promise.reject("Cancelled");
          }

          const originalMetadata = metadata[key];
          const updatedImagePayload = new ImagePayload();
          updatedImagePayload.id = imageId;
          updatedImagePayload.projectId = projectId;
          updatedImagePayload.fileName = originalMetadata.fileName;
          updatedImagePayload.owner = originalMetadata.owner;
          updatedImagePayload.experiment = originalMetadata.experiment;
          updatedImagePayload.sample = originalMetadata.sample;
          updatedImagePayload.timePoint = originalMetadata.timePoint;
          updatedImagePayload.assayType = originalMetadata.assayType;
          updatedImagePayload.mic = originalMetadata.mic;
          updatedImagePayload.groupRow = originalMetadata.groupRow;
          updatedImagePayload.groupColumn = originalMetadata.groupColumn;
          updatedImagePayload.version = originalMetadata.version;
          updatedImagePayload.pixelSizeMillimeter = originalMetadata.pixelSizeMillimeter;
          updatedImagePayload.metadata = originalMetadata.metadata;
          updatedImagePayload.maskImageAnnotations = originalMetadata.maskImageAnnotations;
          updatedImagePayload.size = originalMetadata.size;

          console.log(`Updating image metadata for ${key}=${imageId} ...`);
          updatePromises.push(
            api.post(`/image/${imageId}/update`, updatedImagePayload).then(() => {
              processedItems++;
              onProgress((processedItems / totalItems) * 100, `Updated ${processedItems}/${totalItems} image metadata...`);
            })
          );
        }

        return Promise.all(updatePromises);
      }).then(() => {
        // After all images are uploaded, we go through the each annotation type ['plate', 'strip-disk', 'zoi-shape'] and do the following:
        const annotationTypes = ['plate', 'strip-disk', 'zoi-shape'];
        const annotationPromises: Promise<void>[] = [];

        for (const annotationType of annotationTypes) {
          if (onCancel()) {
            return Promise.reject("Cancelled");
          }

          for (const [key, imageId] of Object.entries(idMapping)) {
            const annotationEntry = zip.file(`${annotationType}/${key}.png`);
            if (!annotationEntry) {
              continue; // No annotation for this image/type combination
            }

            // Use the same uploading mechanism as for the raw images above since backend expects @RequestPart("file") MultipartFile
            annotationPromises.push(
              annotationEntry.async("blob").then((annotationBlob) => {
                console.log(`Uploading annotation ${key}=${imageId}/${annotationType} ...`);
                const formData = new FormData();
                formData.append("file", annotationBlob, `${annotationType}/${key}.png`);

                return api.post(`/mask-image-annotation/${imageId}/${annotationType}/raw`, formData, {
                  headers: {
                    'Content-Type': 'multipart/form-data'
                  }
                }).then(() => {
                  processedItems++;
                  onProgress((processedItems / totalItems) * 100, `Uploaded ${processedItems}/${totalItems} annotations...`);
                });
              })
            );
          }
        }

        return Promise.all(annotationPromises);
      }).then(() => {
        onProgress(100, "ZIP upload completed successfully!");
      });
    });
  })
}
