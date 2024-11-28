export type KonvaEvent<T> = { evt: T };
export type Position = { x: number; y: number };

export enum MouseEventType {
  LeftMouseDown,
  LeftMouseUp,
  LeftMouseClick,
  RightMouseClick,
  MouseMove,
  MouseEnter,
  MouseLeave,
  LeftMouseDoubleClick,
}
