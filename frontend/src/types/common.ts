import { ClassConstructor, plainToInstance } from 'class-transformer';
import { api } from 'boot/axios';
import { Ref } from 'vue';
import { AssayType } from 'src/types/assayType';
import { ImagePayload } from 'src/types/image';
import {Loading} from "quasar";

export function renderMaskAnnotationId(image: ImagePayload, id: string) {
  switch (id) {
    case 'strip-disk':
      switch (image.assayType) {
        case AssayType.DDA:
          return 'Disk';
        case AssayType.ETest:
          return 'Strip';
      }
      break;
    case 'zoi-shape':
      return 'ZOI shape';
    case 'plate':
      return 'Plate';
  }
  return id;
}

export function renderMaskAnnotationId2(id: string) {
  switch (id) {
    case 'strip-disk':
      return "DDA disk / ETest strip"
    case 'zoi-shape':
      return 'ETest ZOI shape';
    case 'plate':
      return 'Plate';
  }
  return id;
}

export function showLoadingWithTimeout(timeout: number, message: string) {
  Loading.show({ message: message });
  setTimeout(() => {
    Loading.hide();
  }, timeout);
}

export function plainToInstanceStrict<T, V>(
  cls: ClassConstructor<T>,
  plain: V
): T {
  return plainToInstance(cls, plain, {
    excludeExtraneousValues: true,
    exposeUnsetFields: false,
  });
}

export function loadPayloadInstanceFromApi<T>(
  url: string,
  type: ClassConstructor<T>,
  target: Ref<T | T[]>
): Promise<T> {
  return new Promise<T>(async (resolve, reject) => {
    api
      .get(url)
      .then((response) => {
        const value = plainToInstance(type, response.data);
        target.value = value;
        resolve(value);
      })
      .catch(reject);
  });
}

export function loadDataStringFromApi(url: string) {
  return new Promise<string>((resolve) => {
    api.get(url, { responseType: 'blob' }).then((response) => {
      resolve(URL.createObjectURL(response.data));
    });
  });
}

export function loadImageElementFromDataString(
  data: Blob | MediaSource
): Promise<HTMLImageElement> {
  return new Promise<HTMLImageElement>((resolve) => {
    const backgroundImageURL = URL.createObjectURL(data);
    const imageObj = new Image();
    imageObj.src = backgroundImageURL;
    imageObj.onload = () => {
      resolve(imageObj);
    };
  });
}

export function downloadDataString(
  dataString: string,
  fileName: string = 'image.png'
) {
  const aDownloadLink = document.createElement('a');
  aDownloadLink.download = fileName;
  aDownloadLink.href = dataString;
  aDownloadLink.click();
}

export function downloadFromApi(
  url: string,
  fileName: string = 'image.png'
): Promise<void> {
  return api.get(url, { responseType: 'blob' }).then((response) => {
    const objectURL = URL.createObjectURL(response.data);
    downloadDataString(objectURL, fileName);
    URL.revokeObjectURL(objectURL);
  });
}

export function removeExtensionIfPresent(
  fileName: string,
  extensions: string[] = [
    '.png',
    '.bmp',
    '.jpg',
    '.jpeg',
    '.tif',
    '.tiff',
    '.zip',
    '.jip',
  ]
): string {
  for (const extension of extensions) {
    if (fileName.toLowerCase().endsWith(extension.toLowerCase())) {
      fileName = fileName.substring(0, fileName.length - extension.length);
    }
  }
  return fileName;
}

export function ensureExtension(
  fileName: string,
  extensions: string[] = [
    '.png',
    '.bmp',
    '.jpg',
    '.jpeg',
    '.tif',
    '.tiff',
    '.zip',
    '.jip',
  ]
) {
  for (const extension of extensions) {
    if (fileName.toLowerCase().endsWith(extension.toLowerCase())) {
      return fileName;
    }
  }
  return fileName + extensions[0];
}

export function uploadImage(url: string, dataUri: string): Promise<void> {
  // Extract the base64 data from the data URI
  const base64Data = dataUri.split(',')[1];
  const byteCharacters = atob(base64Data);
  const byteNumbers = new Array(byteCharacters.length);

  for (let i = 0; i < byteCharacters.length; i++) {
    byteNumbers[i] = byteCharacters.charCodeAt(i);
  }

  const byteArray = new Uint8Array(byteNumbers);
  const blob = new Blob([byteArray], { type: 'image/png' });

  // Create FormData and append the image
  const formData = new FormData();
  formData.append('file', blob, 'image.png');

  // Perform the Axios POST request
  return api.post(url, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
}

