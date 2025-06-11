import { app, BrowserWindow } from 'electron';
import path from 'path';
import os from 'os';
import { spawn } from 'child_process';
import * as net from 'net';
import { ChildProcess } from 'node:child_process';

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

async function startSpringBoot(): Promise<number> {
  const port = await findFreePort();

  const appPath = app.getAppPath(); // Quasar's build path
  const backendDir = path.join(appPath, '..', '..', 'backend-electron'); // Adjust as needed
  const jarPath = path.join(backendDir, 'backend.jar');

  console.log("App path is " + appPath)
  console.log("Backend dir is " + backendDir)

  springBootProcess = spawn('/usr/bin/java', ['-jar', jarPath, `--server.port=${port}`, "--auth.disableAuth=true"], {
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
