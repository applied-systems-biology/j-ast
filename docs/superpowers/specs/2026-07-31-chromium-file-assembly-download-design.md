# Chromium File Assembly Download

**Date:** 2026-07-31
**Status:** Approved

## Problem

The server-side ZIP download system splits results into multiple ZIP parts. On multi-part downloads, users must manually click each part link in a dialog — no auto-download, no save-location choice, no transfer progress. Single-part downloads auto-download but also lack progress and save-location control.

The File System Access API (`showSaveFilePicker` + `createWritable`), available in Chromium browsers (Chrome, Edge) and Electron, enables a MEGA-style flow: stream parts directly to a file on disk, with zero RAM buildup, real-time progress, and user-chosen save location. Firefox lacks this API and needs a graceful fallback.

## Design

### Dual-mode server

New enum `DownloadBundleMode`:
- `SPLIT_ZIP` — generate one ZIP, split raw bytes into N part files. Client reassembles into one `.zip`.
- `SEPARATE_ZIPS` — current behavior: N self-contained ZIP files.

`DownloadBundle` entity gains:
```java
@Enumerated(EnumType.STRING)
private DownloadBundleMode mode = DownloadBundleMode.SEPARATE_ZIPS;

private String outputFileName;  // final .zip name for SPLIT_ZIP mode, e.g. "MyResult.zip"
```

Default `SEPARATE_ZIPS` preserves backward compatibility.

`DownloadBundlePayload` gains `@JsonProperty mode` and `@JsonProperty outputFileName`.

`prepareDownload` endpoint gains a `mode` query param:
```
POST /api/result/{id}/prepare-download?path=/&mode=SPLIT_ZIP
```

### SPLIT_ZIP generation

When `mode == SPLIT_ZIP`, `generateBundle` takes a different path:

1. **Generate one ZIP** to a temp file — all matching items, same entry-naming and filtering logic as current `SEPARATE_ZIPS` implementation.
2. **Split raw bytes** into N part files at `downloadConfig.getMaxPartSizeMb() * 1_000_000L` threshold. Each part file stored via `FileStorageService` with UUID filename.
3. **Part naming**: `baseName.zip.part1`, `baseName.zip.part2`, etc.
4. **Part metadata**: each `DownloadBundlePart` has `fileId`, `fileName`, `size` (same structure as now). Bundle stores `outputFileName = baseName + ".zip"`.
5. **Progress**: "Creating ZIP" (item-by-item, percent based on item count) then "Splitting into parts" (byte-level, percent based on bytes written / total ZIP size).
6. **Crash recovery**: same incremental part persistence as current — after each part is written, `bundle.getParts()` is updated and saved. Partial in-progress part file is deleted on failure. Already-completed parts are retained and downloadable.
7. **Cleanup**: `cleanupExpired` and `resetStuckBundles` work unchanged — part files are deleted regardless of mode.

When `mode == SEPARATE_ZIPS`, the existing `splitIntoParts` + per-part ZIP generation runs unchanged.

New method:
```java
private List<DownloadBundlePart> splitZipIntoParts(Path zipFile, String baseName,
                                                    Path storageDir, String bundleId)
```
Reads the temp ZIP in chunks of `maxPartSizeMb * 1_000_000L` bytes, writes each chunk to a UUID-named file, returns the part list. Deletes the temp ZIP after splitting.

### Frontend feature detection

```typescript
function supportsFileSystemAccess(): boolean {
  return typeof window !== 'undefined' && 'showSaveFilePicker' in window;
}
```

### Download flow changes in ResultsViewLayout.vue

**`downloadZip(path)`:**
- Feature-detect `showSaveFilePicker`.
- Chromium → `POST prepare-download?mode=SPLIT_ZIP`.
- Firefox → `POST prepare-download?mode=SEPARATE_ZIPS` + one-time notice: "For large downloads, Chrome/Edge or the desktop app provides a better experience (save-location selection, automatic file assembly)."

**`pollDownloadBundle` on `Ready`:**

Chromium (`SPLIT_ZIP` mode):
1. `showSaveFilePicker({ suggestedName: bundle.outputFileName })` — user picks one save location.
2. `const writable = await fileHandle.createWritable()`.
3. For each part: `fetch(partUrl)`, stream `response.body` through `writable.write()` in chunks, tracking bytes for per-part progress.
4. `await writable.close()`.
5. Progress dialog: "Downloading part 2/5 — 67%" with overall progress bar.
6. On completion: dialog shows "Download complete!" with single OK button.
7. Cancel: `reader.cancel()` + `writable.abort()`.

Firefox (`SEPARATE_ZIPS` mode):
- Current behavior unchanged: auto-download single part, manual links for multi-part.

### Streaming download implementation

```typescript
async function streamPartsToFile(
  bundle: DownloadBundlePayload,
  fileHandle: FileSystemFileHandle,
  accessToken: string,
  onProgress: (part: number, totalParts: number, partPercent: number, overallPercent: number) => void
): Promise<void> {
  const writable = await fileHandle.createWritable();
  const totalParts = bundle.parts.length;
  const totalSize = bundle.parts.reduce((sum, p) => sum + p.size, 0);
  let receivedTotal = 0;

  for (let i = 0; i < totalParts; i++) {
    const response = await fetch(
      `${apiBase}/download-bundle/${bundle.id}/part/${i}?token=${encodeURIComponent(accessToken)}`
    );
    const reader = response.body!.getReader();
    let received = 0;
    const partSize = bundle.parts[i].size;

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      await writable.write(value);
      received += value.byteLength;
      receivedTotal += value.byteLength;
      onProgress(
        i + 1,
        totalParts,
        Math.round((received / partSize) * 100),
        Math.round((receivedTotal / totalSize) * 100)
      );
    }
  }

  await writable.close();
}
```

Key properties:
- **No RAM buildup** — each chunk is written to disk then garbage-collected. Memory stays flat regardless of file size.
- **Per-part + overall progress** — `onProgress` fires on every chunk.
- **Sequential** — parts downloaded in order, written to one file handle, producing one valid `.zip`.
- **Cancellable** — user cancel triggers `reader.cancel()` + `writable.abort()`.

### Electron desktop app

Electron runs Chromium, so `showSaveFilePicker` is available. No changes to main process or preload — the renderer's web API call works directly and shows the native OS file dialog. `showSaveFilePicker` presence is the correct gate; `isDesktopApp()` is not needed.

## Out of Scope

- No changes to `SEPARATE_ZIPS` generation logic (existing behavior preserved exactly).
- No changes to the existing `downloadFromApi` / `downloadResultItem` single-file download paths.
- No browser detection beyond `showSaveFilePicker` feature detection (no userAgent sniffing).
- No OPFS or service worker complexity for Firefox.
- No changes to `ProjectLayout.vue` download flows (JSZip path) — only `ResultsViewLayout.vue` server-side ZIP downloads.
