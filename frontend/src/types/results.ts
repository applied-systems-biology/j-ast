import { Expose, Type } from 'class-transformer';
import { Dialog } from 'quasar';
import ResultItemViewerDialog from 'components/dataViewers/ResultItemViewerDialog.vue';

export enum ResultItemType {
  Image = "Image",
  Table= "Table",
  Unknown = "Unknown",
}

export class ResultPayload {
  @Expose()
  id: number = -1;

  @Expose()
  projectId: number = -1;

  @Expose()
  name: string = "";

  @Expose()
  description: string = "";

  @Expose()
  createdAt: string = "";

  @Expose()
  viewed: boolean = false;
}

export class ResultItemPayload {
  @Expose()
  id: number = -1;

  @Expose()
  resultId: number = -1;

  @Expose()
  projectId: number = -1;

  @Expose()
  name: string = "";

  @Expose()
  path: string = "";

  @Expose()
  metadata : Record<string, any> = {};

  @Expose()
  type: ResultItemType = ResultItemType.Unknown;

  @Expose()
  visualizationType: ResultItemType = ResultItemType.Unknown;

  /**
   * Allows to override the backend URL
   */
  overrideUrl: string | undefined = undefined;

  /**
   * Allows to override the backend URL
   */
  overrideVisualizationUrl: string | undefined = undefined;

  /**
   * Returns the backend URL that contains the visualization
   */
  getVisualizationUrl() : string {
    if(this.overrideVisualizationUrl) {
      return this.overrideVisualizationUrl;
    }
    else if(this.overrideUrl) {
      return this.overrideUrl;
    }
    else if(this.visualizationType != ResultItemType.Unknown) {
      return `/result-item/${this.id}/visualization`;
    }
    else {
      return `/result-item/${this.id}/raw`;
    }
  }
}


export class FullResultPayload extends ResultPayload {
  @Expose()
  @Type(() => ResultItemPayload)
  items: ResultItemPayload[] = [];

  constructor() {
    super();
  }
}


export function showResultItem(resultItem: ResultItemPayload) {
  Dialog.create({
    component: ResultItemViewerDialog,
    componentProps: {
      resultItem: resultItem,
      persistent: true,
    },
  })
    .onOk(() => {
    })
    .onCancel(() => {
    })
    .onDismiss(() => {
    });
}
