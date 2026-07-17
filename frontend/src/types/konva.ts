/*
 * Copyright (c) 2026.
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

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
