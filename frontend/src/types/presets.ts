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

import {Expose} from "class-transformer";

export class StripPresetPayload {
    @Expose()
    id: number = 0;

    @Expose()
    name: string = '';

    @Expose()
    ticks: Array<number> = [];

    @Expose()
    values: Array<number> = [];

    getName(): string {
        return this.name || "Unnamed";
    }

    getEffectiveTicks(): Array<number> {
        if (this.ticks && this.ticks.length > 0) {
            return this.ticks;
        }
        if (this.values && this.values.length > 0) {
            return this.values;
        }
        return [];
    }

    isPresent() : boolean {
        return this.getEffectiveTicks().length > 0;
    }

    ticksMatch(other: StripPresetPayload): boolean {
        const a = this.getEffectiveTicks();
        const b = other.getEffectiveTicks();
        if (a.length !== b.length) return false;
        for (let i = 0; i < a.length; i++) {
            if (a[i] !== b[i]) return false;
        }
        return true;
    }
}
