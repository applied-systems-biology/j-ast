# Server-Side ZIP Download (Google Drive Style)

## Problem

The frontend builds ZIP files entirely in browser memory using JSZip (`zip.generateAsync({ type: 'blob' })`). For large results (6GB+), this causes:

1. **`TypeError: can't access property "$", a is null`** — Quasar progress dialog double-hide bug. `ProjectLayout.vue` and `IndexPage.vue` call `dialog.hide()` in `onCancel` AND unconditionally in `.finally()`. The second call on an already-dismissed dialog throws a TypeError.
2. **`Failed to generate ZIP file: Bug : can't construct the Blob`** — JSZip tries to build the entire 6GB ZIP in memory, exceeding browser memory limits.

Additionally, the previous streaming ZIP approach (added as a quick fix) is unreliable: no `Content-Length` header means the browser cannot verify download completeness, cannot resume interrupted downloads, and network hiccups produce silently corrupted ZIPs.

## Solution

Replace in-browser JSZip with a server-side background ZIP generation system modeled after Google Drive's download approach:

1. User requests download
2. Server starts a background job that generates ZIP file(s) to disk
3. Frontend polls for progress
4. When ready, server serves the file(s) with `Content-Length` and `Range` support for reliable, resumable downloads
5. If total size exceeds 2GB, results are split into multiple ZIP parts
6. Generated files expire after 1 hour and are cleaned up by a scheduled task

## Design

### Data Model

**New entity: `DownloadBundle`**
```
DownloadBundle
  id: String (UUID, primary key)
  result: ManyToOne<Result>  (for authorization + loading items)
  status: DownloadBundleStatus enum (Preparing | Ready | Failed | Expired)
  path: String  (sub-path filter, e.g. "/" or "/folder1")
  createdAt: LocalDateTime
  expiresAt: LocalDateTime  (createdAt + 1 hour)
  totalSize: long  (total bytes of all result items)
  partCount: int
  parts: List<DownloadBundlePart>  (stored as JSON column via JsonType)
  progressPercent: int  (0-100, updated during generation)
  progressMessage: String  (e.g. "Creating part 2 of 3 (45%)")
  errorMessage: String  (set if Failed)
```

**Embedded class: `DownloadBundlePart`**
```
DownloadBundlePart
  fileId: String  (FileStorageService UUID)
  fileName: String  (e.g. "result_part1.zip")
  size: long  (file size in bytes)
```

**Enum: `DownloadBundleStatus`**
```
Preparing | Ready | Failed | Expired
```

ZIP files are stored in `FileStorageService` under UUIDs (same as all other blobs). `DownloadBundlePart.fileId` references them. This reuses existing storage infrastructure.

The `progressPercent` and `progressMessage` fields are updated in-place during the background job, so the frontend can poll them directly.

### Backend Services

**`DownloadBundleService`**

- `prepareDownload(resultId, path, authentication)`:
  1. Validate auth, load `Result`, check `canAccess`
  2. Create `DownloadBundle` (status=Preparing, expiresAt=now+1h)
  3. Enqueue JobRunr job: `generateBundle(bundleId)`
  4. Return bundle

- `generateBundle(bundleId)` (JobRunr job, `@Transactional(REQUIRES_NEW)`):
  1. Load `ResultItem`s matching the path filter
  2. Calculate total size, determine number of parts: `ceil(totalSize / 1.9GB)`
  3. For each part:
     a. Generate a UUID file ID, create `ZipOutputStream` writing directly to `FileStorageService`'s storage directory at `storageLocation/{uuid}`
     b. Stream files into the ZIP with an 8KB buffer
     c. After each file, update `progressPercent` and `progressMessage` on the entity and save
     d. Close ZIP, record `DownloadBundlePart` (fileId, fileName, size)
  4. Set status=Ready, save part metadata
  5. On error: set status=Failed with errorMessage

- `getBundle(bundleId)`:
  1. Load bundle, check `result.getProject().canAccess(authentication)`
  2. Return bundle payload

- `servePart(bundleId, partIndex, response)`:
  1. Load bundle, check access
  2. Get `DownloadBundlePart` at index
  3. Resolve file path via `fileStorageService.getFilePath(part.fileId)`
  4. Set `Content-Type: application/zip`, `Content-Disposition: attachment`, `Content-Length`, `Accept-Ranges: bytes`
  5. Stream file to response (supports Range requests for resumable downloads)

- `cleanupExpired()` (`@Scheduled(fixedRate = 5 minutes)`):
  1. Find all bundles where `expiresAt < now()` and status is not Expired
  2. Delete all part files via `fileStorageService.delete(part.fileId)`
  3. Set status=Expired

**Splitting logic:**
- Threshold: 1.9GB per part (accounts for ZIP overhead)
- Greedy fill: iterate result items sorted by path, add to current part until the next file would exceed 1.9GB, then start a new part
- Each part is a valid standalone ZIP
- Naming: `{resultName}_part1.zip`, `{resultName}_part2.zip`, etc. (or just `{resultName}.zip` if single part)

**FileStorageService change:**
- Expose `getStorageLocation()` (already exists) so `DownloadBundleService` can write ZIP files directly to the storage directory. The service generates a UUID file ID, writes the ZIP to `storageLocation/{uuid}`, and stores the UUID as `DownloadBundlePart.fileId`. No new `store` method needed — the ZIP is written in-place rather than copied.

### Backend Endpoints

**New controller: `DownloadBundleController`**

| Endpoint | Method | Purpose |
|---|---|---|
| `/api/result/{id}/prepare-download?path=...` | POST | Start ZIP generation, returns `DownloadBundlePayload` |
| `/api/download-bundle/{bundleId}` | GET | Poll status + progress + part list |
| `/api/download-bundle/{bundleId}/part/{partIndex}` | GET | Download a ZIP part (Content-Length + Range) |

Authorization: `prepare-download` checks `result.getProject().canAccess()`. `getBundle` and `servePart` re-check access via the bundle's result reference. Download URLs include `?token=` query parameter for browser-managed downloads (supported by the JwtTokenFilter change that accepts tokens from query params).

### Frontend UX

**Download flow in `ResultsViewLayout.vue`:**

1. User clicks "Download everything" or "Download current folder"
2. Confirmation dialog: shows file count and total size
3. On OK: `POST /api/result/{id}/prepare-download?path=...` returns `bundleId`
4. Progress dialog (spinner + message), polls `GET /api/download-bundle/{bundleId}` every 2 seconds
   - Displays `progressMessage` (e.g. "Creating part 2 of 3 (45%)")
   - Cancel button available (stops polling, bundle expires naturally)
5. When `status === Ready`:
   - **Single part:** auto-trigger download via anchor tag, show "Download ready!" with a manual re-download link
   - **Multiple parts:** show "Download ready! (N parts, ~2GB each)" with a button per part
6. Dialog dismisses after user clicks "Done"

**New frontend types:**
- `DownloadBundlePayload` — id, status, progressPercent, progressMessage, totalSize, partCount, parts[]
- `DownloadBundlePartPayload` — fileName, size

**Removal:** The old `generateAndDownloadResultsZip` function in `results.ts` is no longer called from `ResultsViewLayout.vue`. Left in place (not deleted) in case other code references it.

### Bug Fixes (retained from prior work)

1. **Double `dialog.hide()` in `ProjectLayout.vue`** — `downloadZip()` and `downloadProjectArchive()` both call `dialog.hide()` in `onCancel` AND unconditionally in `.finally()`. Fixed by removing `hide()` from `onCancel` and guarding `.finally()` with `if (!shouldCancel.value)` (matching the pattern already used in `ResultsViewLayout.vue`).

2. **Double `dialog.hide()` in `IndexPage.vue`** — Same bug in `doUploadProjectArchive()`. Same fix.

3. **`JwtTokenFilter` query parameter support** — Already implemented. Allows browser-managed downloads (anchor tags) to pass the JWT token as `?token=` since they cannot set HTTP headers.

### Files to Create

**Backend:**
- `model/entities/DownloadBundle.java` — JPA entity
- `model/entities/DownloadBundlePart.java` — Embedded part metadata (or nested in DownloadBundle)
- `model/DownloadBundleStatus.java` — Enum
- `repositories/DownloadBundleRepository.java` — Spring Data repository
- `services/DownloadBundleService.java` — Lifecycle management + JobRunr job
- `controller/DownloadBundleController.java` — REST endpoints
- `payloads/downloadbundle/DownloadBundlePayload.java` — API payload
- `payloads/downloadbundle/DownloadBundlePartPayload.java` — Part payload

**Backend files to modify:**
- `services/FileStorageService.java` — Already added `getFilePath()`. No further changes needed (uses existing `getStorageLocation()`).
- `utils/JwtTokenFilter.java` — Already modified to accept `?token=` query parameter
- `controller/ResultsController.java` — Remove the streaming ZIP endpoint (replaced by DownloadBundleController)

**Frontend files to modify:**
- `layouts/ResultsViewLayout.vue` — Replace JSZip download flow with polling + download links
- `layouts/ProjectLayout.vue` — Double-hide fix (already applied)
- `pages/IndexPage.vue` — Double-hide fix (already applied)

**Frontend files to create:**
- `types/downloadBundle.ts` — TypeScript payloads for download bundle API

## Non-Goals

- ZIP download for project inputs/images (only results for now)
- User-specific bundle quotas or rate limiting
- ZIP encryption or password protection
- Progress via WebSocket (polling is sufficient)
