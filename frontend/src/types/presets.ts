import {Expose} from "class-transformer";

export class StripPresetPayload {
    @Expose()
    id: number = 0;

    @Expose()
    name: string = '';

    @Expose()
    ticks: Array<number> = [];
}