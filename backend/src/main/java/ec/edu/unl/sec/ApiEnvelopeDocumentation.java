package ec.edu.unl.sec;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Document the same transport transformation applied by ApiResponseAdvice. */
final class ApiEnvelopeDocumentation {
    private ApiEnvelopeDocumentation() {}

    @SuppressWarnings({"rawtypes", "unchecked"})
    static void apply(OpenAPI api, boolean oidc) {
        if (api.getComponents() == null) api.setComponents(new io.swagger.v3.oas.models.Components());
        api.getComponents().addSecuritySchemes("sessionCookie", new io.swagger.v3.oas.models.security.SecurityScheme()
                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.APIKEY).in(io.swagger.v3.oas.models.security.SecurityScheme.In.COOKIE).name("SEC_SESSION"));
        if (oidc) api.getComponents().addSecuritySchemes("bearerAuth", new io.swagger.v3.oas.models.security.SecurityScheme()
                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"));
        api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
            boolean login = path.equals("/sessions") && method == io.swagger.v3.oas.models.PathItem.HttpMethod.POST;
            operation.setSecurity(login ? List.of() : oidc ? List.of(
                    new io.swagger.v3.oas.models.security.SecurityRequirement().addList("sessionCookie"),
                    new io.swagger.v3.oas.models.security.SecurityRequirement().addList("bearerAuth"))
                    : List.of(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("sessionCookie")));
            if (method == io.swagger.v3.oas.models.PathItem.HttpMethod.POST && Set.of("/sessions", "/careers", "/academic-units").contains(path)) {
                var success = operation.getResponses().remove("200");
                if (success != null) operation.getResponses().addApiResponse("201", success);
            }
            if (path.equals("/sessions") && method == io.swagger.v3.oas.models.PathItem.HttpMethod.DELETE) {
                operation.getResponses().remove("200");
                operation.getResponses().addApiResponse("204", new ApiResponse().description("Sesión revocada; sin cuerpo."));
            }
            operation.getResponses().forEach((code, response) -> {
                if (code.equals("204") || code.equals("304") || response.getContent() == null) return;
                boolean error = code.equals("default") || (code.matches("\\d{3}") && Integer.parseInt(code) >= 400);
                new java.util.ArrayList<>(response.getContent().entrySet()).forEach(entry -> {
                    var media = entry.getValue();
                    Schema schema = media.getSchema();
                    if (schema != null && !"binary".equals(schema.getFormat()) && !"byte".equals(schema.getFormat())) {
                        media.setSchema(envelope(api, schema, error));
                        if (entry.getKey().equals("*/*") || entry.getKey().equals("application/problem+json")) {
                            response.getContent().remove(entry.getKey());
                            response.getContent().addMediaType("application/json", media);
                        }
                    }
                });
            });
            for (String code : List.of("400", "401", "403", "404", "409", "412", "422", "426", "428", "429", "500")) {
                if (!operation.getResponses().containsKey(code)) {
                    operation.getResponses().addApiResponse(code, new ApiResponse()
                            .description("Error de validación, acceso, dominio, transporte o límite de solicitudes; conservar el código HTTP.")
                            .content(new Content().addMediaType("application/json", new MediaType().schema(envelope(api, new ObjectSchema(), true)))));
                }
            }
        }));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Schema envelope(OpenAPI api, Schema original, boolean error) {
        Schema data = original;
        var meta = new ObjectSchema();
        Schema resolved = original;
        if (resolved.get$ref() != null && api.getComponents().getSchemas() != null) {
            resolved = api.getComponents().getSchemas().getOrDefault(resolved.get$ref().substring(resolved.get$ref().lastIndexOf('/') + 1), original);
        }
        if (!error && resolved.getProperties() != null && resolved.getProperties().containsKey("page")) {
            Map<String, Schema> properties = new LinkedHashMap<>(resolved.getProperties());
            for (String key : List.of("page", "size", "total", "totalElements", "totalPages", "hasNext")) {
                if (properties.containsKey(key)) meta.addProperty(key, properties.remove(key));
            }
            var object = new ObjectSchema(); object.setProperties(properties); data = object;
        } else if (!error && (resolved instanceof ArraySchema || "array".equals(resolved.getType()))) {
            for (String key : List.of("page", "size", "totalElements", "totalPages")) meta.addProperty(key, new IntegerSchema());
        }
        var result = new ObjectSchema();
        result.setRequired(List.of("success", "message", "data", "errors", "meta"));
        result.setAdditionalProperties(false);
        result.addProperty("success", new BooleanSchema()._const(!error));
        result.addProperty("message", new StringSchema());
        result.addProperty("data", error ? new Schema().type("null") : data);
        result.addProperty("errors", error ? new ObjectSchema() : new Schema().type("null"));
        result.addProperty("meta", meta);
        return result;
    }
}
