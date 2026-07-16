# Electron Distribution Fixes

**Date:** 2026-07-16
**Status:** Approved
**Approach:** A — Direct port from kite_publication_1

## Problem

The J-AST Electron desktop distribution has several issues that make the packaged apps non-functional:

1. **Frontend does not wait for the server to start.** The Electron main process spawns the Java backend and immediately loads the BrowserWindow. The renderer polls `/ping` every 1 second via `useIntervalFn` in `UserManagerComponent.vue`, but the user sees a blank window until Vue mounts and the polling succeeds.

2. **Windows Firewall confirmation.** The `findFreePort()` function binds to `0.0.0.0` (all interfaces), and the Spring Boot backend also binds to all interfaces by default. This triggers the Windows Firewall confirmation dialog on first launch.

3. **Linux sandbox prevents startup.** The packaged Linux app includes a `chrome-sandbox` binary that requires root-owned setuid permissions, which is impossible to distribute in a tar.gz. Without it, Chromium aborts with "No usable sandbox!" on systems that restrict unprivileged user namespaces (e.g., Ubuntu 23.10+ with AppArmor). `app.commandLine.appendSwitch('no-sandbox')` does not work because the browser-process sandbox initialization happens before V8/Node.js runs.

4. **No graceful shutdown.** The `before-quit` handler sends `SIGTERM` to the Java process, which can corrupt the H2 database.

5. **No error feedback.** If the backend crashes or fails to start, there is no user-visible error — the app just shows a blank window.

6. **No splash screen.** The user sees a blank white window until the JS bundle loads and Vue mounts.

7. **Bug: missing `break` in Java path switch.** The `linux` case in the Java path switch statement (line 120 of `electron-main.ts`) falls through to `default`, logging an error.

These issues were solved in the kite_publication_1 project. This spec ports the proven solutions to J-AST.

## Design

### Section 1: Electron Main Process (`frontend/src-electron/electron-main.ts`)

Seven modifications to the existing file:

**1b. Localhost-only port binding** — Change `findFreePort()` to bind to `127.0.0.1` instead of the default `0.0.0.0`:

```typescript
server.listen(0, '127.0.0.1', () => {
```

**1c. Server readiness wait** — Add a `waitForBackend(port, timeoutMs = 30000)` function that polls `http://localhost:{port}/` every 500ms using Node's `http` module. In `createWindow()`, call `await waitForBackend(springPort)` after `startSpringBoot()` and before creating the `BrowserWindow`. On timeout, show `dialog.showErrorBox` but still create the window so the user sees something.

```typescript
function waitForBackend(port: number, timeoutMs = 30000): Promise<void> {
  const start = Date.now();
  return new Promise((resolve, reject) => {
    const check = () => {
      if (Date.now() - start > timeoutMs) {
        reject(new Error(`Backend did not start within ${timeoutMs}ms`));
        return;
      }
      const req = http.get(`http://localhost:${port}/`, (res) => {
        res.destroy();
        resolve();
      });
      req.on('error', () => {
        setTimeout(check, 500);
      });
      req.end();
    };
    check();
  });
}
```

Add `import http from 'node:http';` at the top.

In `createWindow()`:
```typescript
springPort = await startSpringBoot();

try {
  await waitForBackend(springPort);
} catch (err) {
  dialog.showErrorBox(
    'Backend Error',
    `The J-AST backend failed to start: ${err instanceof Error ? err.message : String(err)}`
  );
}
```

**1d. Windows Firewall fix** — Add `address: '127.0.0.1'` to the `server` section of the runtime `application.yml` config object:

```typescript
const appConfig = {
  server: {
    port: port,
    address: '127.0.0.1',
  },
  // ...
}
```

This makes Spring Boot bind to localhost only, preventing the Windows Firewall dialog.

**1e. Graceful shutdown** — Add actuator config to the runtime `application.yml`:

```typescript
management: {
  endpoint: {
    shutdown: {
      enabled: true,
    },
  },
  endpoints: {
    web: {
      exposure: {
        include: 'shutdown',
      },
    },
  },
},
```

Replace the `before-quit` handler:

```typescript
app.on('before-quit', async () => {
  if (!springBootProcess) return;
  try {
    await new Promise<void>((resolve) => {
      const req = http.request(
        `http://localhost:${springPort}/actuator/shutdown`,
        { method: 'POST' },
        () => resolve()
      );
      req.on('error', () => resolve());
      req.end();
    });
    await new Promise((resolve) => setTimeout(resolve, 2000));
  } catch {
    // Ignore — fall through to SIGTERM
  }
  springBootProcess.kill('SIGTERM');
});
```

**1f. Error handlers** — Add exit and error handlers on `springBootProcess`:

```typescript
springBootProcess.on('exit', (code) => {
  if (code !== 0 && code !== null) {
    dialog.showErrorBox(
      'Backend Error',
      `The J-AST backend exited with code ${code}. Check the console for details.`
    );
  }
});

springBootProcess.on('error', (err) => {
  dialog.showErrorBox(
    'Backend Error',
    `Failed to start the J-AST backend: ${err.message}`
  );
});
```

**1g. Bug fix** — Add the missing `break` in the `linux` case of the Java path switch statement:

```typescript
case "linux":
  javaProcess = path.join(backendDir, "jdk", "bin", "java")
  break
default:
```

**What stays the same:** The existing renderer-side `/ping` polling in `UserManagerComponent.vue` stays as-is. It becomes a near-instant fallback since the server is already up when the window loads. The `additionalArguments` mechanism for passing the API base URL, the preload script, and the desktop mode detection logic all remain unchanged.

### Section 2: Backend (`backend/pom.xml`)

Add `spring-boot-starter-actuator` dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

The actuator config (shutdown endpoint enabled) is only in the Electron runtime `application.yml` written by `electron-main.ts`. In web mode, the shutdown endpoint is not exposed by default (Spring Boot defaults), so this does not affect the web deployment.

### Section 3: Frontend Splash Screen (`frontend/index.html` + `frontend/src/App.vue`)

**`frontend/index.html`** — Add a static `<div id="app-splash">` in `<body>` before `<!-- quasar:entry-point -->`, with inline CSS for a centered splash with J-AST branding and a CSS spinner. The splash uses J-AST's orange color scheme to match the existing loading dialog.

```html
<style>
  #app-splash {
    position: fixed;
    inset: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: #fafafa;
    z-index: 9999;
  }
  #app-splash .splash-text {
    font-size: 28px;
    font-weight: 700;
    letter-spacing: 2px;
    color: #e65100;
  }
  #app-splash .splash-spinner {
    margin-top: 24px;
    width: 36px;
    height: 36px;
    border: 3px solid #ffe0b2;
    border-top-color: #e65100;
    border-radius: 50%;
    animation: splash-spin 0.8s linear infinite;
  }
  @keyframes splash-spin {
    to { transform: rotate(360deg); }
  }
</style>
```

```html
<body>
  <div id="app-splash">
    <div class="splash-text">J-AST</div>
    <div class="splash-spinner"></div>
  </div>
  <!-- quasar:entry-point -->
</body>
```

**`frontend/src/App.vue`** — Add an `onMounted` hook to remove the splash:

```typescript
import { onMounted } from 'vue';

onMounted(() => {
  const splash = document.getElementById('app-splash');
  if (splash) {
    splash.remove();
  }
});
```

The existing `UserManagerComponent.vue` q-dialog loading indicator stays as a secondary indicator.

### Section 4: Build Script Linux Sandbox Fix (`dist/electron/build.sh`)

Add the Linux sandbox wrapper after copying the packaged Electron output (after the existing `cp -r ./dist/electron/Packaged/J-AST-linux-x64` line), before the `popd`:

```bash
# Linux: remove chrome-sandbox (requires root-owned setuid, impossible in a
# tar.gz) and wrap the Electron binary with --no-sandbox so Chromium doesn't
# abort with "No usable sandbox!" on systems that also restrict unprivileged
# user namespaces (e.g. Ubuntu 23.10+ with AppArmor). The switch must be on
# the real command line — app.commandLine.appendSwitch() in electron-main.ts
# only affects renderer processes, not the browser-process sandbox init that
# happens before V8 runs.
rm -f "$TMP_DIR/j-ast-linux-x64/chrome-sandbox"
mv "$TMP_DIR/j-ast-linux-x64/J-AST" "$TMP_DIR/j-ast-linux-x64/J-AST-bin"
cat > "$TMP_DIR/j-ast-linux-x64/J-AST" << 'WRAPPER'
#!/bin/bash
DIR="$(dirname "$(readlink -f "$0")")"
exec "$DIR/J-AST-bin" --no-sandbox "$@"
WRAPPER
chmod +x "$TMP_DIR/j-ast-linux-x64/J-AST"
```

macOS is unaffected — the sandbox issue is Linux-specific.

No changes to `.gitlab-ci.yml` — it calls `build.sh` which now includes the fix.

## Files Changed

| File | Change |
|------|--------|
| `frontend/src-electron/electron-main.ts` | Add `waitForBackend()`, localhost bind, `server.address`, actuator config, error handlers, graceful shutdown, fix `break` bug |
| `backend/pom.xml` | Add `spring-boot-starter-actuator` dependency |
| `frontend/index.html` | Add static HTML splash screen |
| `frontend/src/App.vue` | Add `onMounted` to remove splash |
| `dist/electron/build.sh` | Add Linux `--no-sandbox` wrapper script |

## What Does NOT Change

- `UserManagerComponent.vue` — renderer-side `/ping` polling stays as fallback
- `frontend/src/types/electron.ts` — desktop mode detection stays as-is
- `frontend/src/types/api.ts` — API base URL resolution stays as-is
- `frontend/src-electron/electron-preload.ts` — preload script stays as-is
- `frontend/src/boot/axios.ts` — axios setup stays as-is
- `frontend/src/stores/auth-store.ts` — auth store stays as-is
- `frontend/quasar.config.js` — Quasar config stays as-is
- `.gitlab-ci.yml` — CI pipeline stays as-is (calls build.sh)
- macOS packaging — unaffected by Linux sandbox fix
