import { Expose } from 'class-transformer';

export class ProviderInfoPayload {
  @Expose()
  providerName: string = "";

  @Expose()
  providerUrl: string = "";
}