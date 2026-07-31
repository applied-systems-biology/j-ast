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

export enum DownloadBundleStatus {
  Preparing = "Preparing",
  Ready = "Ready",
  Failed = "Failed",
  Expired = "Expired",
}

export enum DownloadBundleMode {
  SEPARATE_ZIPS = "SEPARATE_ZIPS",
  SPLIT_ZIP = "SPLIT_ZIP",
}

export class DownloadBundlePartPayload {
  fileName: string = "";
  size: number = 0;
}

export class DownloadBundlePayload {
  id: string = "";
  status: DownloadBundleStatus = DownloadBundleStatus.Preparing;
  progressPercent: number = 0;
  progressMessage: string = "";
  totalSize: number = 0;
  partCount: number = 0;
  parts: DownloadBundlePartPayload[] = [];
  errorMessage: string = "";
  mode: DownloadBundleMode = DownloadBundleMode.SEPARATE_ZIPS;
  outputFileName: string = "";
}
