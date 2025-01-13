import { Expose, Type } from 'class-transformer';
import { Dialog } from 'quasar';
import ResultItemViewerDialog from 'components/dataViewers/ResultItemViewerDialog.vue';
import JSZip from "jszip";
import {api} from "boot/axios";
import {sendFailureNotification} from "src/types/notification";
import {makeFilesystemCompatible} from "src/types/utils";
import {ensureExtension} from "src/types/common";

export enum ResultItemType {
  Image = "Image",
  Table= "Table",
  Unknown = "Unknown",
}

export class ResultPayload {
  @Expose()
  id: number = -1;

  @Expose()
  projectId: number = -1;

  @Expose()
  name: string = "";

  @Expose()
  description: string = "";

  @Expose()
  createdAt: string = "";

  @Expose()
  viewed: boolean = false;
}

export class ResultItemPayload {
  @Expose()
  id: number = -1;

  @Expose()
  resultId: number = -1;

  @Expose()
  projectId: number = -1;

  @Expose()
  name: string = "";

  @Expose()
  path: string = "";

  @Expose()
  metadata : Record<string, any> = {};

  @Expose()
  type: ResultItemType = ResultItemType.Unknown;

  @Expose()
  visualizationType: ResultItemType = ResultItemType.Unknown;

  @Expose()
  size: number = 0;

  /**
   * Allows to override the backend URL
   */
  overrideUrl: string | undefined = undefined;

  /**
   * Allows to override the backend URL
   */
  overrideVisualizationUrl: string | undefined = undefined;

  /**
   * Returns the backend URL that contains the visualization
   */
  getVisualizationUrl() : string {
    if(this.overrideVisualizationUrl) {
      return this.overrideVisualizationUrl;
    }
    else if(this.overrideUrl) {
      return this.overrideUrl;
    }
    else if(this.visualizationType != ResultItemType.Unknown) {
      return `/result-item/${this.id}/visualization`;
    }
    else {
      return `/result-item/${this.id}/raw`;
    }
  }
}


export class FullResultPayload extends ResultPayload {
  @Expose()
  @Type(() => ResultItemPayload)
  items: ResultItemPayload[] = [];

  constructor() {
    super();
  }
}


export function showResultItem(resultItem: ResultItemPayload) {
  Dialog.create({
    component: ResultItemViewerDialog,
    componentProps: {
      resultItem: resultItem,
      persistent: true,
    },
  })
    .onOk(() => {
    })
    .onCancel(() => {
    })
    .onDismiss(() => {
    });
}

export function generateAndDownloadResultsZip(
  items: ResultItemPayload[],
  subPath: string,
  fileName: string,
  onProgress: (progress: number, info: string) => void,
  onCancel: () => boolean
): Promise<void> {
  const zip = new JSZip();
  const totalItems = items.length;
  let completedItems = 0;
  let cancelled = false;

  // handle the case where we have a subPath
  while(subPath.startsWith("/")) {
    subPath = subPath.substring(1)
  }

  const downloadPromises = items.map((item) => {
    if (cancelled) return Promise.resolve();

    return api.get(`result/${item.id}/raw`, { responseType: 'blob' })
      .then((response) => {
        if (onCancel()) {
          console.log('Download cancelled.');
          cancelled = true;
          return;
        }

        let relativePath = `${item.path}/${item.name}`;

        if(subPath && relativePath.startsWith(subPath)) {
          relativePath = relativePath.substring(subPath.length);
        }

        // Fix relative path
        while(relativePath.startsWith("/")) {
          relativePath = relativePath.substring(1);
        }

        zip.file(relativePath, response.data);
        completedItems++;
        const progress = Math.round((completedItems / totalItems) * 100);
        onProgress(progress, `Downloading files (${completedItems} / ${totalItems})`);
      })
      .catch((error) => {
        if (onCancel()) {
          cancelled = true;
          console.log('Download cancelled.');
          return;
        }

        sendFailureNotification("Failed to download file with ID " + item.id);
        console.error(`Failed to download file with ID ${item.id}:`, error);
        completedItems++;
        const progress = Math.round((completedItems / totalItems) * 100);
        onProgress(progress, `Downloading files (${completedItems} / ${totalItems})`);
      });
  });

  return Promise.all(downloadPromises)
    .then(() => {
      if (cancelled) {
        sendFailureNotification("ZIP file generation cancelled!");
        console.log('ZIP generation cancelled.');
        return;
      }
      return zip.generateAsync({ type: 'blob' }, ({ percent }) => {
        onProgress(Math.round(percent), "Creating ZIP file");
      });
    })
    .then((zipBlob) => {
      if (cancelled) return;
      if (!zipBlob) {
        sendFailureNotification("Error while generating zip file!");
        return;
      }

      const link = document.createElement('a');
      link.href = URL.createObjectURL(zipBlob);
      link.download = ensureExtension(makeFilesystemCompatible(fileName || "result.zip"), [".zip"]);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    })
    .catch((error) => {
      sendFailureNotification("Error while generating zip file!");
      console.error('Failed to generate ZIP file:', error);
    });
}
