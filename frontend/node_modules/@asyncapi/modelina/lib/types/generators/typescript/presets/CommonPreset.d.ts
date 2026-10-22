import { TypeScriptPreset } from '../TypeScriptPreset';
export interface TypeScriptCommonPresetOptions {
    marshalling: boolean;
    example: boolean;
}
/**
 * Preset which adds `marshal`, `unmarshal`, `example` functions to class.
 *
 * @implements {TypeScriptPreset}
 */
export declare const TS_COMMON_PRESET: TypeScriptPreset<TypeScriptCommonPresetOptions>;
//# sourceMappingURL=CommonPreset.d.ts.map