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

export interface Badge {
  text: string;
  color: string;
  icon: string;
  type: string;
}

export function createExperimentBadge(value: string): Badge {
  return {
    text: value,
    icon: 'fa-solid fa-vial-virus',
    color: '#62a0ea',
    type: 'Experiment'
  };
}

export function createSampleBadge(value: string): Badge {
  return {
    text: value,
    icon: 'fa-solid fa-flask',
    color: '#33d17a',
    type: 'Sample'
  };
}

export function createTimePointBadge(value: string): Badge {
  return {
    text: value,
    icon: 'fa-solid fa-clock',
    color: '#e5a50a',
    type: 'Time point'
  };
}

export function createAssayTypeBadge(value: string): Badge {
  return {
    text: value,
    icon: 'fa-solid fa-gear',
    color: '#9141ac',
    type: 'Assay type'
  };
}

export function createPixelSizeBadge(value: string): Badge {
  return {
    text: value,
    icon: 'fa-solid fa-ruler',
    color: '#41acac',
    type: 'Assay type'
  };
}

export function createStripPresetBadge(value: string): Badge {
    return {
        text: value,
        icon: 'fa-solid fa-ruler-vertical',
        color: '#ac4141',
        type: 'Strip preset'
    };
}

export function createCustomMetadataBadge(key: string, value: string): Badge {
  return {
    text: `${key}: ${value}`,
    icon: 'fa-solid fa-tag',
    color: '#888888',
    type: 'custom-metadata/' + key
  };
}
