import { ZipItem } from 'src/types/zip';
import { ensureExtension } from 'src/types/common';
import { plainToInstance } from 'class-transformer';
import { ImagePayload } from 'src/types/image';
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

    // TODO 2: For each key in the metadata do the following: Get the image data from ZIP (entry is known: /[key].png within the ZIP). Then we upload each image to ${apiBase}/project/${projectId}/upload-raw-image
    // TODO 2: You will need to make changes to ProjectController so we can get ImagePayload and the respective IDs back. Keep track which record key and which image payload ID correspond

    // TODO 3: Now as we now the image IDs, post for each image the ${apiBase}/image/{id}/update with the updated ImagePayload where metadata was taken from the Record

    // TODO 4: After all images are uploaded, we go through the each annotation type ['plate', 'strip-disk', 'zoi-shape'] and do the following:
    // TODO 4: Check if there's a zip entry /[annotation type]/[key].png. If it exists, upload it using  uploadImage(
    //       `/mask-image-annotation/${image id in database}/${annotation type}/raw`,
    //       pngData
    //     ) where the pngData is a DataURL('image/png')

    // Keep reporting progress through onProgress (number is percent)
    // Listen to onCancel for cancellation

  })
}