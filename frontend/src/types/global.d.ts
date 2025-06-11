// src/types/global.d.ts
export {};

declare global {
  interface Window {
    electronAPI?: {
      getApiBase: () => string | null;
    };
  }
}
