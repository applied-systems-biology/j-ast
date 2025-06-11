/**
 * This file is used specifically for security reasons.
 * Here you can access Nodejs stuff and inject functionality into
 * the renderer thread (accessible there through the "window" object)
 *
 * WARNING!
 * If you import anything from node_modules, then make sure that the package is specified
 * in package.json > dependencies and NOT in devDependencies
 *
 * Example (injects window.myAPI.doAThing() into renderer thread):
 *
 *   import { contextBridge } from 'electron'
 *
 *   contextBridge.exposeInMainWorld('myAPI', {
 *     doAThing: () => {}
 *   })
 *
 * WARNING!
 * If accessing Node functionality (like importing @electron/remote) then in your
 * electron-main.ts you will need to set the following when you instantiate BrowserWindow:
 *
 * mainWindow = new BrowserWindow({
 *   // ...
 *   webPreferences: {
 *     // ...
 *     sandbox: false // <-- to be able to import @electron/remote in preload script
 *   }
 * }
 */

// Inject any static/default value as fallback
import { contextBridge } from 'electron';

function getArgValue(prefix: string): string | null {
  const arg = process.argv.find(arg => arg.startsWith(prefix));
  return arg ? arg.slice(prefix.length) : null;
}

const apiBase = getArgValue('--api-base=');

// Expose getApiBase
contextBridge.exposeInMainWorld('electronAPI', {
  getApiBase: () => {
    console.log("getApiBase() accessed");
    console.log(apiBase)
    return apiBase;
  },
});
