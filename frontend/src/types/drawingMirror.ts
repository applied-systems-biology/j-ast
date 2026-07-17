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

export enum MirrorOperationMode {
  Max = 'Max',
  Min = 'Min',
  AboveOrLeft = 'AboveOrLeft',
  BelowOrRight = 'BelowOrRight',
}

function mirrorImageDataHorizontal(
  canvas: HTMLCanvasElement,
  x1: number,
  y1: number,
  x2: number,
  y2: number,
  mirrorSide: MirrorOperationMode,
  data: Uint8ClampedArray
) {
  // Horizontal mirror line
  const mirrorY = y1;
  for (let y = 0; y < canvas.height; y++) {
    for (let x = 0; x < canvas.width; x++) {
      const mirrorYPos = 2 * mirrorY - y;
      if (mirrorYPos >= 0 && mirrorYPos < canvas.height) {
        const index1 = (y * canvas.width + x) * 4;
        const index2 = (mirrorYPos * canvas.width + x) * 4;

        if (mirrorSide === MirrorOperationMode.AboveOrLeft && y < mirrorY) {
          // Copy mirrored data upwards
          for (let i = 0; i < 4; i++) {
            data[index1 + i] = data[index2 + i];
          }
        } else if (
          mirrorSide === MirrorOperationMode.BelowOrRight &&
          y > mirrorY
        ) {
          // Copy mirrored data downwards
          for (let i = 0; i < 4; i++) {
            data[index2 + i] = data[index1 + i];
          }
        } else if (mirrorSide === MirrorOperationMode.Max) {
          // Take the maximum value of both sides
          for (let i = 0; i < 4; i++) {
            data[index1 + i] = Math.max(data[index1 + i], data[index2 + i]);
            data[index2 + i] = data[index1 + i];
          }
        } else if (mirrorSide === MirrorOperationMode.Min) {
          // Take the maximum value of both sides
          for (let i = 0; i < 4; i++) {
            data[index1 + i] = Math.min(data[index1 + i], data[index2 + i]);
            data[index2 + i] = data[index1 + i];
          }
        }
      }
    }
  }
}

function mirrorImageDataVertical(
  canvas: HTMLCanvasElement,
  x1: number,
  y1: number,
  x2: number,
  y2: number,
  mirrorSide: MirrorOperationMode,
  data: Uint8ClampedArray
) {
  // Vertical mirror line
  const mirrorX = x1;
  for (let y = 0; y < canvas.height; y++) {
    for (let x = 0; x < canvas.width; x++) {
      const mirrorXPos = 2 * mirrorX - x;
      if (mirrorXPos >= 0 && mirrorXPos < canvas.width) {
        const index1 = (y * canvas.width + x) * 4;
        const index2 = (y * canvas.width + mirrorXPos) * 4;

        if (mirrorSide === MirrorOperationMode.AboveOrLeft && x < mirrorX) {
          // Copy mirrored data to the left
          for (let i = 0; i < 4; i++) {
            data[index1 + i] = data[index2 + i];
          }
        } else if (
          mirrorSide === MirrorOperationMode.BelowOrRight &&
          x > mirrorX
        ) {
          // Copy mirrored data to the right
          for (let i = 0; i < 4; i++) {
            data[index2 + i] = data[index1 + i];
          }
        } else if (mirrorSide === MirrorOperationMode.Max) {
          // Take the maximum value of both sides
          for (let i = 0; i < 4; i++) {
            data[index1 + i] = Math.max(data[index1 + i], data[index2 + i]);
            data[index2 + i] = data[index1 + i];
          }
        } else if (mirrorSide === MirrorOperationMode.Min) {
          // Take the maximum value of both sides
          for (let i = 0; i < 4; i++) {
            data[index1 + i] = Math.min(data[index1 + i], data[index2 + i]);
            data[index2 + i] = data[index1 + i];
          }
        }
      }
    }
  }
}

// Function to reflect a point (px, py) across the line
function reflect(
  px: number,
  py: number,
  a: number,
  b: number,
  c: number
): [number, number] {
  // Correct calculation for reflection
  const scale = (a * px + b * py + c) / (a * a + b * b);
  const rx = px - 2 * a * scale;
  const ry = py - 2 * b * scale;
  return [rx, ry];
}

function mirrorImageDataAny(
  canvas: HTMLCanvasElement,
  x1: number,
  y1: number,
  x2: number,
  y2: number,
  mirrorSide: MirrorOperationMode,
  data: Uint8ClampedArray
) {
  // Calculate line coefficients: ax + by + c = 0
  const a = y2 - y1;
  const b = x1 - x2;
  const c = x2 * y1 - x1 * y2;

  // Loop through every pixel on the canvas
  for (let y = 0; y < canvas.height; y++) {
    for (let x = 0; x < canvas.width; x++) {
      const index = (y * canvas.width + x) * 4;

      // Reflect the current pixel position across the line
      const [rx, ry] = reflect(x, y, a, b, c);

      // Ensure the reflected position is within the bounds of the canvas
      if (rx >= 0 && rx < canvas.width && ry >= 0 && ry < canvas.height) {
        const mirrorIndex =
          (Math.round(ry) * canvas.width + Math.round(rx)) * 4;

        // Handle the mirror operations
        for (let i = 0; i < 4; i++) {
          if (mirrorSide === MirrorOperationMode.AboveOrLeft) {
            // Copy mirrored pixel to the current pixel
            data[index + i] = data[mirrorIndex + i];
          } else if (mirrorSide === MirrorOperationMode.BelowOrRight) {
            // Copy current pixel to the mirrored pixel
            data[mirrorIndex + i] = data[index + i];
          } else if (mirrorSide === MirrorOperationMode.Max) {
            // Take the maximum value of both pixels
            const maxValue = Math.max(data[index + i], data[mirrorIndex + i]);
            data[index + i] = maxValue;
            data[mirrorIndex + i] = maxValue;
          } else if (mirrorSide === MirrorOperationMode.Min) {
            // Take the maximum value of both pixels
            const maxValue = Math.min(data[index + i], data[mirrorIndex + i]);
            data[index + i] = maxValue;
            data[mirrorIndex + i] = maxValue;
          }
        }
      }
    }
  }
}

export function mirrorImageData(
  canvas: HTMLCanvasElement,
  x1: number,
  y1: number,
  x2: number,
  y2: number,
  mirrorSide: MirrorOperationMode
) {
  const context = canvas.getContext('2d');
  if (!context) {
    return;
  }
  const imageData = context.getImageData(0, 0, canvas.width, canvas.height);
  const data = imageData.data;

  if (y1 == y2) {
    mirrorImageDataHorizontal(canvas, x1, y1, x2, y2, mirrorSide, data);
  } else if (x1 == x2) {
    mirrorImageDataVertical(canvas, x1, y1, x2, y2, mirrorSide, data);
  } else {
    mirrorImageDataAny(canvas, x1, y1, x2, y2, mirrorSide, data);
  }

  context.putImageData(imageData, 0, 0);
}
