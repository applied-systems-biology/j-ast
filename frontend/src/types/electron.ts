export function isDesktopApp(): boolean {
  return process.env.MODE === 'electron' ||
    (typeof window !== 'undefined' &&
      typeof window.process === 'object' &&
      window.process.type === 'renderer');
}

export function isWebApp() {
  return !isDesktopApp()
}