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
