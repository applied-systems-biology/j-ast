# Configurable ZIP Splitting

**Date:** 2026-07-31
**Status:** Approved

## Problem

The server-side ZIP download system splits archives into parts at a hardcoded ~1.9 GB threshold (`MAX_PART_SIZE = 2_048_000_000L` in `DownloadBundleService`). With a provider upload rate of only 1.8 MB/s, downloading a ~2 GB part causes timeouts. The split size and bundle expiry time (1 hour, also hardcoded) need to be configurable so operators can tune them for their network conditions.

## Current State

| Concern | Current value | Location | Configurable? |
|---|---|---|---|
| Split part size | 2,048,000,000 bytes (~1.9 GiB) | `DownloadBundleService.MAX_PART_SIZE` (static final) | No |
| Bundle expiry | 1 hour | `DownloadBundleService.prepareDownload()` (literal) | No |
| Copy buffer | 8192 bytes | `DownloadBundleService.BUFFER_SIZE` (static final) | No (stays hardcoded) |
| Cleanup interval | 5 min | `@Scheduled` annotation | No (stays hardcoded) |
| Frontend poll interval | 2000 ms | `ResultsViewLayout.vue` (literal) | No (stays hardcoded) |

The codebase uses `@ConfigurationProperties` exclusively (no `@Value` annotations anywhere). Five config classes exist (`RuntimeConfig`, `AccountConfig`, `JwtConfig`, `ProviderConfig`, `PresetsConfig`), all registered via `@EnableConfigurationProperties` in `JASTWebApplicationServer`.

## Design

### New `DownloadConfig` class

**File:** `backend/src/main/java/org/hkijena/jast/config/DownloadConfig.java`

```java
@Component
@ConfigurationProperties(prefix = "download")
@Validated
public class DownloadConfig {
    @Min(1)
    private long maxPartSizeMb = 2048;

    @Min(1)
    private int expiryHours = 1;

    // getters + setters
}
```

This follows the `AccountConfig` pattern: `@Component` + `@ConfigurationProperties` + `@Validated` with `@Min` constraints to prevent invalid values (zero or negative).

Registered in `JASTWebApplicationServer.@EnableConfigurationProperties`.

**Unit convention:** MB uses the decimal multiplier `1_000_000` (not `1024 * 1024`). This preserves the current semantics — the existing constant `2_048_000_000L` equals `2048 * 1_000_000`, not a power-of-two value.

### Service changes (`DownloadBundleService`)

1. Remove the `MAX_PART_SIZE` static constant (line 55). Keep `BUFFER_SIZE` as-is.
2. Constructor-inject `DownloadConfig` alongside existing dependencies.
3. In `splitIntoParts()`: replace `MAX_PART_SIZE` with `downloadConfig.getMaxPartSizeMb() * 1_000_000L`.
4. In `prepareDownload()`: replace `plusHours(1)` with `plusHours(downloadConfig.getExpiryHours())`.
5. The greedy bin-packing algorithm itself is unchanged — only the threshold is parameterized.

### YAML configuration

Add to `backend/src/main/resources/application.yml`:

```yaml
download:
  max-part-size-mb: 2048        # Split ZIP parts at this size (in MB). Lower for slow upload links.
  expiry-hours: 1               # Bundles expire after this many hours. Increase for slow downloads.
```

Both fields have defaults in the Java class, so omitting them from YAML is safe. For the 1.8 MB/s provider link, an operator could set `max-part-size-mb: 500` and `expiry-hours: 6`.

### Frontend

No changes needed. The frontend already handles any number of parts dynamically — `updated.parts.length` drives the link list, and polling/progress/download-link generation all work regardless of part size.

## Out of Scope

- Buffer size, cleanup interval, and poll interval remain hardcoded — not affected by the slow-upload problem.
- No changes to the splitting algorithm (greedy bin-packing stays as-is).
- No database schema changes.
- No frontend changes.
