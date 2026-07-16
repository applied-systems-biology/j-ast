import { app, BrowserWindow } from 'electron';
import os from 'os';
import { spawn } from 'child_process';
import * as net from 'net';
import http from 'node:http';
import { ChildProcess } from 'node:child_process';
import { mkdirSync } from 'fs';
import path from 'path';
import YAML from 'yaml';
import { writeFileSync } from 'node:fs';
import { dialog } from 'electron';
import fs from 'fs';

// needed in case process is undefined under Linux
const platform = process.platform || os.platform();

let mainWindow: BrowserWindow | undefined;
let springBootProcess: ChildProcess | undefined;
let springPort: number;
let isQuitting = false;

function findFreePort(): Promise<number> {
  return new Promise((resolve) => {
    const server = net.createServer();
    server.listen(0, '127.0.0.1', () => {
      const port = (server.address() as net.AddressInfo).port;
      server.close(() => resolve(port));
    });
  });
}

function waitForBackend(port: number, timeoutMs = 30000): Promise<void> {
  const start = Date.now();
  return new Promise((resolve, reject) => {
    const check = () => {
      if (Date.now() - start > timeoutMs) {
        reject(new Error(`Backend did not start within ${timeoutMs}ms`));
        return;
      }
      const req = http.get(`http://127.0.0.1:${port}/`, (res) => {
        res.destroy();
        resolve();
      });
      req.on('error', () => {
        setTimeout(check, 500);
      });
      req.setTimeout(2000, () => {
        req.destroy();
        setTimeout(check, 500);
      });
    };
    check();
  });
}

function getBackendDir(): string {
  const devPath = path.resolve(app.getAppPath(), '..', '..', 'backend-electron');

  // Check if we're running in dev mode
  if (fs.existsSync(devPath)) {
    return devPath;
  }

  // In production, it's inside the unpacked resources
  return path.join(process.resourcesPath, 'backend-electron');
}

async function startSpringBoot(): Promise<number> {
  const port = await findFreePort();

  const appPath = app.getAppPath(); // Quasar's build path
  const backendDir = getBackendDir()
  const jarPath = path.join(backendDir, 'backend.jar');

  console.log("App path is " + appPath)
  console.log("Backend dir is " + backendDir)

  // Storage files/directories
  const dataDirectory = path.join(backendDir, "storage", "data");
  const tmpDirectory = path.join(backendDir, "storage", "tmp");
  const databaseFile = path.join(backendDir, "storage", "database");
  const appConfigPath = path.join(backendDir, "application.yml");

  // Setup configuration
  const appConfig = {
    server: {
      port: port,
      address: '127.0.0.1',
    },
    spring: {
      datasource: {
        url: "jdbc:h2:file:" + databaseFile,
        username: "sa",
        password: "password",
        driverClassName: "org.h2.Driver"
      }
    },
    runtime: {
      dataDirectory: dataDirectory,
      customTempDirectory: tmpDirectory,
      sharedResourcesDirectory: path.resolve(backendDir, 'share'),
      keepTmp: false
    },
    accounts: {
      disableAuth: true
    },
    management: {
      endpoint: {
        shutdown: {
          enabled: true
        }
      },
      endpoints: {
        web: {
          exposure: {
            include: 'shutdown'
          }
        }
      }
    }
  }

  // Configure JIPipe
  switch (os.platform()) {
    case "win32":
      (appConfig as any)["runtime"]["fijiPath"] = path.join(backendDir, "jipipe-windows", "bin");
      (appConfig as any)["runtime"]["fijiExecutablePath"] = path.join(backendDir, "jipipe-windows", "bin", "ImageJ-win64.exe");
      (appConfig as any)["runtime"]["fijiWrapperEnabled"] = false
      break
    case "darwin":
      (appConfig as any)["runtime"]["fijiPath"] = path.join(backendDir, "jipipe-macos", "Contents", "Resources", "bin");
      (appConfig as any)["runtime"]["fijiExecutablePath"] = path.join(backendDir, "jipipe-macos", "Contents", "Resources", "bin", "Fiji.app", "Contents", "MacOS", "fiji-macos-x64");
      (appConfig as any)["runtime"]["fijiWrapperEnabled"] = false
      break
    case "linux":
      (appConfig as any)["runtime"]["fijiPath"] = path.join(backendDir, "jipipe-linux", "bin");
      (appConfig as any)["runtime"]["fijiExecutablePath"] = path.join(backendDir, "jipipe-linux", "bin", "ImageJ-linux64");
      (appConfig as any)["runtime"]["fijiWrapperEnabled"] = false
      break
    default:
      console.error("UNABLE TO DETERMINE CURRENT PLATFORM, RETURNED " + os.platform())
      break
  }

  // Prepare directories & write config
  mkdirSync(dataDirectory, { recursive: true });
  mkdirSync(tmpDirectory, { recursive: true });

  writeFileSync(appConfigPath, YAML.stringify(appConfig));

  // Create Spring process
  let javaProcess : string | undefined;
  switch(os.platform()) {
    case "win32":
      javaProcess = path.join(backendDir, "jdk", "bin", "java.exe")
      break
    case "darwin":
      javaProcess = path.join(backendDir, "jdk", "Contents", "Home", "bin", "java")
      break
    case "linux":
      javaProcess = path.join(backendDir, "jdk", "bin", "java")
      break
    default:
      console.error("UNABLE TO DETERMINE CURRENT PLATFORM, RETURNED " + os.platform())
      break
  }

  springBootProcess = spawn(javaProcess!, [ "-jar", jarPath, `--spring.config.additional-location=${appConfigPath}` ], {
    cwd: backendDir,
    stdio: 'inherit',
  });

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

  return port;
}

async function createWindow() {

  springPort = await startSpringBoot();

  try {
    await waitForBackend(springPort);
  } catch (err) {
    dialog.showErrorBox(
      'Backend Error',
      `The J-AST backend failed to start: ${err instanceof Error ? err.message : String(err)}`
    );
  }

  /**
   * Initial window options
   */
  mainWindow = new BrowserWindow({
    icon: path.resolve(__dirname, 'icons/icon.png'), // tray icon
    width: 1800,
    height: 1000,
    useContentSize: true,
    webPreferences: {
      contextIsolation: true,
      sandbox: false,
      // More info: https://v2.quasar.dev/quasar-cli-vite/developing-electron-apps/electron-preload-script
      preload: path.resolve(__dirname, process.env.QUASAR_ELECTRON_PRELOAD),
      additionalArguments: [`--api-base=http://127.0.0.1:${springPort}/api`],
    },
  });

  mainWindow.setMenuBarVisibility(false);
  mainWindow.loadURL(process.env.APP_URL);

  if (process.env.DEBUGGING) {
    // if on DEV or Production with debug enabled
    mainWindow.webContents.openDevTools();
  } else {
    // we're on production; no access to devtools pls
    mainWindow.webContents.on('devtools-opened', () => {
      mainWindow?.webContents.closeDevTools();
    });
  }

  mainWindow.on('close', (event) => {
    const choice = dialog.showMessageBoxSync(mainWindow!, {
      type: 'question',
      buttons: ['Cancel', 'Quit'],
      defaultId: 1,
      cancelId: 0,
      title: 'Confirm Exit',
      message: 'Are you sure you want to quit J-AST? All running processes will be cancelled.',
    });

    if (choice === 0) {
      // User clicked "Cancel"
      event.preventDefault(); // Prevent window from closing
    }
  });

  mainWindow.on('closed', () => {
    mainWindow = undefined;
  });
}

app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
  if (platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (mainWindow === undefined) {
    createWindow();
  }
});

app.on('before-quit', (event) => {
  if (isQuitting || !springBootProcess) return;
  event.preventDefault();
  isQuitting = true;

  const req = http.request(
    `http://127.0.0.1:${springPort}/actuator/shutdown`,
    { method: 'POST' },
    () => {
      setTimeout(() => {
        springBootProcess?.kill('SIGTERM');
        app.exit(0);
      }, 2000);
    }
  );
  req.on('error', () => {
    springBootProcess?.kill('SIGTERM');
    app.exit(0);
  });
  req.setTimeout(5000, () => {
    req.destroy();
    springBootProcess?.kill('SIGTERM');
    app.exit(0);
  });
  req.end();
});
