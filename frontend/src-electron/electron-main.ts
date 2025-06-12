import { app, BrowserWindow } from 'electron';
import os from 'os';
import { spawn } from 'child_process';
import * as net from 'net';
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

function findFreePort(): Promise<number> {
  return new Promise((resolve) => {
    const server = net.createServer();
    server.listen(0, () => {
      const port = (server.address() as net.AddressInfo).port;
      server.close(() => resolve(port));
    });
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
    }
  }

  // Configure JIPipe
  switch (os.platform()) {
    case "win32":
      (appConfig as any)["runtime"]["fijiPath"] = path.join(backendDir, "bin", "jipipe-windows");
      (appConfig as any)["runtime"]["fijiExecutablePath"] = path.join(backendDir, "bin", "jipipe-windows", "ImageJ-linux64.exe");
      (appConfig as any)["runtime"]["fijiWrapperEnabled"] = false
      break
    case "darwin":
      break
    case "linux":
      (appConfig as any)["runtime"]["fijiPath"] = path.join(backendDir, "bin", "jipipe-linux");
      (appConfig as any)["runtime"]["fijiExecutablePath"] = path.join(backendDir, "bin", "jipipe-linux", "ImageJ-linux64");
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
  springBootProcess = spawn('/usr/bin/java', [ "-jar", jarPath, `--spring.config.additional-location=${appConfigPath}` ], {
    cwd: backendDir,
    stdio: 'inherit',
  });

  return port;
}

async function createWindow() {

  springPort = await startSpringBoot();

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
      additionalArguments: [`--api-base=http://localhost:${springPort}/api`],
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

app.on('before-quit', () => {
  if (springBootProcess) {
    springBootProcess.kill('SIGTERM');
  }
});
