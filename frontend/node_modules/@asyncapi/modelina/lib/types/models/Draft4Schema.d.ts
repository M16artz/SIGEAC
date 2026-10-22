/**
 * JSON Draft 4 schema model
 */
export declare class Draft4Schema {
    $schema?: string;
    title?: string;
    multipleOf?: number;
    maximum?: number;
    exclusiveMaximum?: boolean;
    minimum?: number;
    exclusiveMinimum?: boolean;
    maxLength?: number;
    minLength?: number;
    pattern?: string;
    maxItems?: number;
    minItems?: number;
    uniqueItems?: boolean;
    maxProperties?: number;
    minProperties?: number;
    allOf?: Draft4Schema[];
    oneOf?: Draft4Schema[];
    anyOf?: Draft4Schema[];
    not?: Draft4Schema;
    dependencies?: {
        [key: string]: Draft4Schema | string[];
    };
    format?: string;
    definitions?: {
        [key: string]: Draft4Schema;
    };
    description?: string;
    default?: any;
    type?: string | string[];
    enum?: any[];
    items?: Draft4Schema | Draft4Schema[];
    properties?: {
        [key: string]: Draft4Schema;
    };
    additionalProperties?: Draft4Schema | boolean;
    patternProperties?: {
        [key: string]: Draft4Schema;
    };
    $ref?: string;
    required?: string[];
    additionalItems?: Draft4Schema | boolean;
    id?: string;
    /**
     * Takes a deep copy of the input object and converts it to an instance of Draft4Schema.
     *
     * @param object
     */
    static toSchema(object: Record<string, unknown>): Draft4Schema;
    private static internalToSchema;
}
//# sourceMappingURL=Draft4Schema.d.ts.map