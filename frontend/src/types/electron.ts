export function isDesktopApp(): boolean {
  if(typeof process !== 'undefined') {
    return process.env.MODE === 'electron' ||
      (typeof window !== 'undefined' &&
        typeof window.process === 'object' &&
        window.process.type === 'renderer');
  }
  else {
    return false;
  }
}

export function isWebApp() {
  return !isDesktopApp()
}