import JSZip from 'jszip';
import { api } from 'boot/axios';
import { sendFailureNotification } from 'src/types/notification';
import { ensureExtension } from 'src/types/common';
import { makeFilesystemCompatible } from 'src/types/utils';

export interface ZipItem {
  entryName: string;
  url: string | null;
  content:
    | string
    | number[]
    | Uint8Array
    | ArrayBuffer
    | Blob
    | NodeJS.ReadableStream
    | null;
}

export interface ZipProgress {
  progress: number;
  info: string;
}

export function addItemsToZip(
  zip: JSZip,
  items: Array<ZipItem>,
  onProgress: (progress: number, info: string) => void,
  onCancel: () => boolean
) {
  let cancelled = false;
  let completedItems = 0;
  const totalItems = items.length;
  return items.map((item) => {
    if (cancelled) return Promise.resolve();

    let relativePath = item.entryName;

    // Fix relative path
    while (relativePath.startsWith('/')) {
      relativePath = relativePath.substring(1);
    }

    if (item.url) {
      return api
        .get(item.url, { responseType: 'blob' })
        .then((response) => {
          if (onCancel()) {
            console.log('Download cancelled.');
            cancelled = true;
            return;
          }

          zip.file(relativePath, response.data);
          completedItems++;
          const progress = Math.round((completedItems / totalItems) * 100);
          onProgress(
            progress,
            `Downloading files (${completedItems} / ${totalItems})`
          );
        })
        .catch((error) => {
          if (onCancel()) {
            cancelled = true;
            console.log('Download cancelled.');
            return;
          }

          sendFailureNotification('Failed to download file ' + item.url);
          console.error(`Failed to download file with ID ${item.url}:`, error);
          completedItems++;
          const progress = Math.round((completedItems / totalItems) * 100);
          onProgress(
            progress,
            `Downloading files (${completedItems} / ${totalItems})`
          );
        });
    } else if (item.content) {
      return new Promise((resolve, reject) => {
        try {
          zip.file(relativePath, item.content!);
          completedItems++;
          const progress = Math.round((completedItems / totalItems) * 100);
          onProgress(
            progress,
            `Downloading files (${completedItems} / ${totalItems})`
          );
          return resolve(item);
        } catch (error) {
          return reject(error);
        }
      });
    } else {
      console.error('Unable to process item - nothing to do:');
      console.error(item);
    }
  });
}

export function generateAndDownloadZip(
  items: Array<ZipItem>,
  downloadedFileName: string,
  onProgress: (progress: number, info: string) => void,
  onCancel: () => boolean
) {
  const zip = new JSZip();
  return Promise.all(addItemsToZip(zip, items, onProgress, onCancel))
    .then(() => {
      if (onCancel()) {
        sendFailureNotification('ZIP file generation cancelled!');
        console.log('ZIP generation cancelled.');
        return;
      }
      return zip.generateAsync({ type: 'blob' }, ({ percent }) => {
        onProgress(Math.round(percent), 'Creating ZIP file');
      });
    })
    .then((zipBlob) => {
      if (onCancel()) return;
      if (!zipBlob) {
        sendFailureNotification('Error while generating zip file!');
        return;
      }

      const link = document.createElement('a');
      link.href = URL.createObjectURL(zipBlob);
      link.download = ensureExtension(
        makeFilesystemCompatible(downloadedFileName || 'download.zip'),
        ['.zip']
      );
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    })
    .catch((error) => {
      sendFailureNotification('Error while generating zip file!');
      console.error('Failed to generate ZIP file:', error);
    });
}
