import {Expose} from "class-transformer";

export class StripPresetPayload {
    @Expose()
    id: number = 0;

    @Expose()
    name: string = '';

    @Expose()
    ticks: Array<number> = [];

    getName(): string {
        return this.name || "Unnamed";
    }

    isPresent() : boolean {
        return this.ticks.length > 0;
    }
}