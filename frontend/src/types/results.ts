import { Expose, Type } from 'class-transformer';

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
}


export class FullResultPayload extends ResultPayload {
  @Expose()
  @Type(() => ResultItemPayload)
  items: ResultItemPayload[] = [];

  constructor() {
    super();
  }
}
