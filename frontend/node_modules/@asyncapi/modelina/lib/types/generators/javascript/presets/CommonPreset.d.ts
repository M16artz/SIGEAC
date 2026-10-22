import { JavaScriptPreset } from '../JavaScriptPreset';
export interface JavaScriptCommonPresetOptions {
    marshalling: boolean;
    example: boolean;
}
/**
 * Preset which adds `marshal`, `unmarshal` functions to class.
 *
 * @implements {JavaScriptPreset}
 */
export declare const JS_COMMON_PRESET: JavaScriptPreset<JavaScriptCommonPresetOptions>;
//# sourceMappingURL=CommonPreset.d.ts.map