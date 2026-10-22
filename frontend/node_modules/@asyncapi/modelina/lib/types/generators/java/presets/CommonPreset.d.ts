import { JavaPreset } from '../JavaPreset';
export interface JavaCommonPresetOptions {
    equal: boolean;
    hashCode: boolean;
    classToString: boolean;
    marshalling: boolean;
}
/**
 * Preset which adds `equal`, `hashCode`, `toString` functions to class.
 *
 * @implements {JavaPreset}
 */
export declare const JAVA_COMMON_PRESET: JavaPreset<JavaCommonPresetOptions>;
//# sourceMappingURL=CommonPreset.d.ts.map