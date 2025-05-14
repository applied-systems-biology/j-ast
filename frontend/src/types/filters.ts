import { AssayType } from 'src/types/assayType';
import { ImagePayload } from 'src/types/image';

export function filterAppliesToImage(filterText : string | undefined, image: ImagePayload | undefined) {
  if (filterText && filterText.length > 0 && image) {
    const tags = new Set<string>();

    if (image.experiment) {
      tags.add('experiment:' + image.experiment);
    }
    if (image.sample) {
      tags.add('sample:' + image.sample);
    }
    if (image.assayType != AssayType.Unknown) {
      tags.add('assayType:' + image.assayType);
    }
    tags.add(image.fileName);

    let hasPlate = false;
    let hasDiskStrip = false;
    let hasZOIShape = false;

    for (const annotation of image.maskImageAnnotations) {
      if (annotation.version > 0) {
        if (annotation.annotationTypeId == 'plate') {
          hasPlate = true;
        } else if (annotation.annotationTypeId == 'strip-disk') {
          hasDiskStrip = true;
        } else if (annotation.annotationTypeId == 'zoi-shape') {
          hasZOIShape = true;
        }
      }
    }

    tags.add('hasPlate:' + (hasPlate ? 'yes' : 'no'));
    tags.add('hasDiskStrip:' + (hasDiskStrip ? 'yes' : 'no'));
    if (image.assayType == AssayType.ETest) {
      tags.add('hasZOIShape:' + (hasZOIShape ? 'yes' : 'no'));
    }

    // Tags to string
    const searchString = [...tags].join(' ');
    return searchString.includes(filterText);
  } else {
    return true;
  }
}