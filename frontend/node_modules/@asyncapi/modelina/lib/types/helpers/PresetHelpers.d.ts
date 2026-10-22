import { Presets } from '../models';
/**
 * Returns true if and only if a given preset is already included in a list of presets
 * Check is done using referential equality
 * @param presets the list to check
 * @param preset the preset to check for
 */
export declare const hasPreset: <P extends Partial<any> = Partial<any>>(presets: Presets<P>, preset: P) => boolean;
//# sourceMappingURL=PresetHelpers.d.ts.map