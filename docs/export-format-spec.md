# J-AST Export Format Specification

This document specifies the file formats produced and consumed by J-AST's export/import system. It is intended for developers who want to create or parse J-AST archives programmatically.

---

## 1. Project Archive Format (`.jast.zip`)

The **project archive** is the primary round-trip format. It can be exported from J-AST and re-imported to reconstruct a project with all images, annotations, and metadata intact.

**File extension:** `.jast.zip`
**MIME type:** `application/zip`
**File naming convention:** `<projectName>.jast.zip`

### 1.1 ZIP Structure

```
<projectName>.jast.zip
├── metadata.json
├── <imageId1>.png
├── <imageId2>.png
├── <imageId3>.png
├── strip-disk/
│   ├── <imageId1>.png
│   ├── <imageId2>.png
│   └── <imageId3>.png
├── plate/
│   ├── <imageId1>.png
│   └── <imageId3>.png
└── zoi-shape/
    ├── <imageId1>.png
    └── <imageId3>.png
```

### 1.2 File Naming Rules

- **Raw images** are stored at the root of the ZIP as `<imageId>.png`, where `<imageId>` is the string representation of the image's numeric identifier.
- **Annotation masks** are stored in subdirectories named after the annotation type, with the same `<imageId>.png` filename pattern: `<annotationType>/<imageId>.png`.
- All images are in **PNG** format.
- If an image has no annotation of a given type, the corresponding file is simply omitted from that subdirectory.
- An annotation subdirectory is only present if at least one image has an annotation of that type.

### 1.3 Valid Annotation Types

The following annotation type identifiers are valid. They correspond to the subdirectory names within the ZIP:

| Annotation Type ID | Description |
|---|---|
| `strip-disk` | Strip/disk region annotation mask |
| `plate` | Plate region annotation mask |
| `zoi-shape` | Zone of inhibition (ZOI) shape annotation mask |

Only these three annotation types exist. Other subdirectory names will be ignored on import.

### 1.4 `metadata.json` Format

The `metadata.json` file is a JSON object where each key is the stringified `imageId` and each value is an object containing the image metadata.

#### Schema

```json
{
  "<imageId>": {
    "id": number,
    "fileName": string,
    "experiment": string,
    "sample": string,
    "timePoint": string,
    "assayType": string,
    "mic": number,
    "groupRow": number,
    "groupColumn": number,
    "version": number,
    "pixelSizeMillimeter": number,
    "metadata": object
  }
}
```

#### Field Definitions

| Field | Type | Required | Description |
|---|---|---|---|
| `id` | `number` (integer) | Yes | Original image database identifier. Must match the `<imageId>` key and the PNG filename. |
| `fileName` | `string` | Yes | Original uploaded filename (e.g., `"photo1.tiff"`). Used as the display name on import. |
| `experiment` | `string` | Yes | Experiment label. Empty string if unset. |
| `sample` | `string` | Yes | Sample label. Empty string if unset. |
| `timePoint` | `string` | Yes | Time point label. Empty string if unset. |
| `assayType` | `string` (enum) | Yes | One of `"DDA"`, `"ETest"`, or `"Unknown"`. See [Assay Types](#15-valid-assay-types). |
| `mic` | `number` (float) | Yes | Minimum inhibitory concentration value. `0` if unset. |
| `groupRow` | `number` (integer) | Yes | Row position in the timeline grid. `-1` means unsorted/not assigned. |
| `groupColumn` | `number` (integer) | Yes | Column position in the timeline grid. `-1` means unsorted/not assigned. |
| `version` | `number` (integer) | Yes | Image version counter. Starts at `0`. |
| `pixelSizeMillimeter` | `number` (float) | Yes | Pixel size calibration in millimeters. `-1` if unset. |
| `metadata` | `object` | Yes | Arbitrary key-value metadata. Empty object `{}` if unset. May contain a `stripPreset` key for E-Test assays. |

#### Example

```json
{
    "42": {
        "id": 42,
        "fileName": "photo1.tiff",
        "experiment": "Exp1",
        "sample": "SampleA",
        "timePoint": "0h",
        "assayType": "DDA",
        "mic": 0.0,
        "groupRow": 0,
        "groupColumn": 1,
        "version": 3,
        "pixelSizeMillimeter": 0.0254,
        "metadata": {
            "stripPreset": {
                "name": "Default",
                "antibiotic": "Amoxicillin"
            }
        }
    },
    "107": {
        "id": 107,
        "fileName": "photo2.tiff",
        "experiment": "Exp2",
        "sample": "SampleB",
        "timePoint": "24h",
        "assayType": "ETest",
        "mic": 1.5,
        "groupRow": 1,
        "groupColumn": 0,
        "version": 0,
        "pixelSizeMillimeter": -1,
        "metadata": {}
    }
}
```

### 1.5 Valid Assay Types

The `assayType` field in `metadata.json` must be one of the following string values:

| Value | Description |
|---|---|
| `"DDA"` | Disk Diffusion Assay |
| `"ETest"` | E-Test (epsilometer test) |
| `"Unknown"` | Unspecified / not yet classified |

### 1.6 Image Requirements

- All images (raw and annotation masks) must be valid **PNG** files.
- Raw images can be any color depth (grayscale, RGB, RGBA).
- Annotation masks are binary/grayscale PNG images where pixel values encode the annotated region.
- Raw images and their corresponding annotation masks must have the **same dimensions** (width x height).

### 1.7 Creating a Valid Archive Programmatically

To create a `.jast.zip` that J-AST can import:

1. **Assign unique numeric IDs** to each image. These can be any positive integers (e.g., sequential starting from 1). The actual IDs will be reassigned on import; they only need to be unique within the archive.

2. **Write raw images** to the root of the ZIP as `<id>.png`.

3. **Write annotation masks** to `<annotationType>/<id>.png` for each annotation type that applies. Only include files for existing annotations.

4. **Create `metadata.json`** with an entry for every image, using the same `<id>` as both the JSON key and the value of the `id` field.

5. **Ensure consistency**: every image ID referenced in `metadata.json` must have a corresponding `<id>.png` file at the root level. Annotation PNGs are optional.

#### Minimal Example

An archive with a single DDA image and a plate annotation:

```
my-project.jast.zip
├── metadata.json
├── 1.png
└── plate/
    └── 1.png
```

Where `metadata.json` contains:

```json
{
    "1": {
        "id": 1,
        "fileName": "disk_photo.png",
        "experiment": "Experiment1",
        "sample": "Sample1",
        "timePoint": "0h",
        "assayType": "DDA",
        "mic": 0.0,
        "groupRow": -1,
        "groupColumn": -1,
        "version": 0,
        "pixelSizeMillimeter": -1,
        "metadata": {}
    }
}
```

### 1.8 Import Behavior

When a `.jast.zip` archive is imported:

1. The `metadata.json` is read to discover all images and their metadata.
2. Each raw image (`<id>.png`) is uploaded. The server assigns a **new** database ID (the original ID in the archive is not preserved).
3. Image metadata (experiment, sample, timePoint, assayType, etc.) is applied to the newly created image.
4. For each annotation type (`strip-disk`, `plate`, `zoi-shape`), if `<annotationType>/<id>.png` exists in the archive, the annotation mask is uploaded and associated with the new image ID.
5. The `id` and `projectId` fields in `metadata.json` are informational; they are reassigned on import and do not need to match any existing project.

---

## 2. Images + Annotations Download (`.zip`)

This is a **download-only** format for human consumption. It is **not importable** back into J-AST.

**File extension:** `.zip`
**File naming convention:** `<projectName>.zip`

### 2.1 ZIP Structure

```
<projectName>.zip
├── photo1.png
├── photo1_strip-disk.png
├── photo1_plate.png
├── photo1_zoi-shape.png
├── photo2.png
├── photo2_plate.png
└── ...
```

### 2.2 Naming Rules

- Raw images are named by the **original filename** (with extension replaced by `.png`). If the original filename has no recognized extension, `.png` is appended.
- If duplicate filenames exist, the image ID is appended for disambiguation: `<fileName>_<imageId>.png`.
- Annotation masks are named `<fileName>_<annotationTypeId>.png` and placed **flat** in the root directory.
- There is **no `metadata.json`** file.

---

## 3. Results Download (`.zip`)

This is a **download-only** format for analysis task results. It is **not importable** back into J-AST.

**File extension:** `.zip`
**File naming convention:** `<resultName>.zip`

### 3.1 ZIP Structure

The results ZIP mirrors the internal backend result directory structure:

```
<resultName>.zip
├── raw/
│   ├── 42.png
│   └── 107.png
├── strip-disk/
│   ├── 42.png
│   └── 107.png
├── plate/
│   ├── 42.png
│   └── 107.png
├── exported/
│   └── circle filter/
│       ├── minCirc.csv
│       └── ...
└── metadata.csv
```

### 3.2 Content Types

Result items can be of the following types:

| File Extension | Type | Description |
|---|---|---|
| `.png`, `.bmp`, `.jpg`, `.jpeg` | Image | Re-encoded as PNG on the backend |
| `.csv` | Table | Raw bytes |
| `.txt`, `.json`, `.xml` | Text | Raw bytes |
| (other) | Unknown | Raw bytes |

---

## 4. Summary of Format Differences

| Feature | Project Archive (`.jast.zip`) | Images + Annotations (`.zip`) | Results (`.zip`) |
|---|---|---|---|
| Importable | Yes | No | No |
| Contains `metadata.json` | Yes | No | No |
| Image naming | By image ID | By original filename | By image ID |
| Annotation layout | Subdirectories | Flat, suffixed filenames | Subdirectories |
| Annotation types | `strip-disk`, `plate`, `zoi-shape` | Same, as filename suffix | Same, as subdirectories |
| Purpose | Round-trip project transfer | Human download | Analysis result download |

---

## 5. Reference Implementation

The export/import code resides in the following files:

| Component | File |
|---|---|
| Frontend archive collection | `frontend/src/types/projectArchive.ts` |
| Frontend ZIP generation | `frontend/src/types/zip.ts` |
| Frontend metadata schema | `frontend/src/types/image.ts` (`getMetadataAsDict()`) |
| Frontend import upload | `frontend/src/types/projectArchive.ts` (`uploadProjectArchive()`) |
| Backend import endpoint | `backend/src/main/java/org/hkijena/jast/controller/ProjectArchiveController.java` |
| Backend annotation types | `backend/src/main/java/org/hkijena/jast/model/entities/MaskImageAnnotation.java` |
| Backend assay type enum | `backend/src/main/java/org/hkijena/jast/model/AssayType.java` |
| Backend image payload | `backend/src/main/java/org/hkijena/jast/payloads/ImagePayload.java` |
