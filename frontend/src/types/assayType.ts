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

export enum AssayType {
  DDA = 'DDA',
  ETest = 'ETest',
  Unknown = 'Unknown',
}

export function parseAssayType(str: string): AssayType {
  if (str) {
    str = str.toLowerCase();
    if (str == 'dda') {
      return AssayType.DDA;
    } else if (str == 'etest') {
      return AssayType.ETest;
    } else if (str.startsWith('e') && str.endsWith('test')) {
      return AssayType.ETest;
    } else {
      return AssayType.Unknown;
    }
  } else {
    return AssayType.Unknown;
  }
}
