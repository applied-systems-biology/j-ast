export function isDesktopApp(): boolean {
  if (typeof window !== 'undefined') {
    if (typeof window.process === 'object' &&
        window.process?.type === 'renderer') {
      return true;
    }
    if (window.electronAPI) {
      return true;
    }
  }
  return import.meta.env.MODE === 'electron';
}

export function isWebApp() {
  return !isDesktopApp()
}