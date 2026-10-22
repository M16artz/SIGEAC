export declare class SwaggerV2Xml {
    name?: string;
    namespace?: string;
    prefix?: string;
    attribute?: boolean;
    wrapped?: boolean;
    static toXml(object: any): SwaggerV2Xml;
}
export declare class SwaggerV2ExternalDocumentation {
    description?: string;
    url?: string;
    [k: string]: any;
    static toExternalDocumentation(object: any): SwaggerV2ExternalDocumentation;
}
/**
 * OpenAPI 2.0 (Swagger 2.0) schema model
 *
 * Based on Draft 4, but with restricted keywords and definitions
 *
 * Restrictions (keywords not allowed)
 *  - oneOf
 *  - anyOf
 *  - patternProperties
 *  - not
 *
 * https://swagger.io/specification/v2/#schemaObject
 */
export declare class SwaggerV2Schema {
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
    allOf?: SwaggerV2Schema[];
    format?: string;
    definitions?: {
        [key: string]: SwaggerV2Schema;
    };
    description?: string;
    default?: any;
    type?: string | string[];
    enum?: any[];
    items?: SwaggerV2Schema | SwaggerV2Schema[];
    properties?: {
        [key: string]: SwaggerV2Schema;
    };
    additionalProperties?: SwaggerV2Schema | boolean;
    $ref?: string;
    required?: string[];
    id?: string;
    discriminator?: string;
    readOnly?: boolean;
    xml?: SwaggerV2Xml;
    externalDocs?: SwaggerV2ExternalDocumentation;
    example?: any;
    [k: string]: any;
    /**
     * Takes a deep copy of the input object and converts it to an instance of SwaggerV2Schema.
     *
     * @param object
     */
    static toSchema(object: Record<string, unknown>): SwaggerV2Schema;
    private static internalToSchema;
}
//# sourceMappingURL=SwaggerV2Schema.d.ts.map