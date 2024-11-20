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
