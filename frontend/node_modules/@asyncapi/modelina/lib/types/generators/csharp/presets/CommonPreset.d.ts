import { CSharpPreset } from '../CSharpPreset';
export interface CSharpCommonPresetOptions {
    equal: boolean;
    hash: boolean;
}
/**
 * Preset which adds `Equals`, `GetHashCode` functions to class.
 *
 * @implements {CSharpPreset}
 */
export declare const CSHARP_COMMON_PRESET: CSharpPreset<CSharpCommonPresetOptions>;
//# sourceMappingURL=CommonPreset.d.ts.map