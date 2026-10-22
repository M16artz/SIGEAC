/**
 * JSON Draft 6 schema model
 */
export declare class Draft6Schema {
    $schema?: string;
    title?: string;
    multipleOf?: number;
    maximum?: number;
    minimum?: number;
    maxLength?: number;
    minLength?: number;
    pattern?: string;
    maxItems?: number;
    minItems?: number;
    uniqueItems?: boolean;
    maxProperties?: number;
    minProperties?: number;
    allOf?: (Draft6Schema | boolean)[];
    oneOf?: (Draft6Schema | boolean)[];
    anyOf?: (Draft6Schema | boolean)[];
    not?: Draft6Schema | boolean;
    dependencies?: {
        [key: string]: Draft6Schema | boolean | string[];
    };
    format?: string;
    definitions?: {
        [key: string]: Draft6Schema | boolean;
    };
    description?: string;
    default?: any;
    type?: string | string[];
    enum?: any[];
    items?: Draft6Schema | Draft6Schema[] | boolean;
    properties?: {
        [key: string]: Draft6Schema | boolean;
    };
    additionalProperties?: Draft6Schema | boolean;
    patternProperties?: {
        [key: string]: Draft6Schema | boolean;
    };
    $ref?: string;
    required?: string[];
    additionalItems?: Draft6Schema | boolean;
    exclusiveMaximum?: number;
    exclusiveMinimum?: number;
    $id?: string;
    contains?: Draft6Schema | boolean;
    const?: any;
    propertyNames?: Draft6Schema | boolean;
    examples?: any[];
    /**
     * Takes a deep copy of the input object and converts it to an instance of Draft6Schema.
     *
     * @param object
     */
    static toSchema(object: Record<string, unknown>): Draft6Schema;
    private static internalToSchema;
}
//# sourceMappingURL=Draft6Schema.d.ts.map