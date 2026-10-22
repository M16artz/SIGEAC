/**
 * JSON Draft7Schema Draft 7 model
 */
export declare class Draft7Schema {
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
    allOf?: (Draft7Schema | boolean)[];
    oneOf?: (Draft7Schema | boolean)[];
    anyOf?: (Draft7Schema | boolean)[];
    not?: Draft7Schema | boolean;
    dependencies?: {
        [key: string]: Draft7Schema | boolean | string[];
    };
    format?: string;
    definitions?: {
        [key: string]: Draft7Schema | boolean;
    };
    description?: string;
    default?: any;
    type?: string | string[];
    enum?: any[];
    items?: Draft7Schema | Draft7Schema[] | boolean;
    properties?: {
        [key: string]: Draft7Schema | boolean;
    };
    additionalProperties?: Draft7Schema | boolean;
    patternProperties?: {
        [key: string]: Draft7Schema | boolean;
    };
    $ref?: string;
    required?: string[];
    additionalItems?: Draft7Schema | boolean;
    exclusiveMaximum?: number;
    exclusiveMinimum?: number;
    $id?: string;
    contains?: Draft7Schema | boolean;
    const?: any;
    propertyNames?: Draft7Schema | boolean;
    examples?: any[];
    $comment?: string;
    if?: Draft7Schema | boolean;
    then?: Draft7Schema | boolean;
    else?: Draft7Schema | boolean;
    readOnly?: boolean;
    writeOnly?: boolean;
    contentEncoding?: string;
    contentMediaType?: string;
    /**
     * Takes a deep copy of the input object and converts it to an instance of Draft7Schema.
     *
     * @param object
     */
    static toSchema(object: Record<string, unknown>): Draft7Schema;
    private static internalToSchema;
}
//# sourceMappingURL=Draft7Schema.d.ts.map