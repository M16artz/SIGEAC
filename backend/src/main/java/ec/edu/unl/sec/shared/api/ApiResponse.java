package ec.edu.unl.sec.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/** Public JSON transport; domain objects remain inside data. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse(boolean success, String message, Object data, Object errors, Map<String, Object> meta) {
    public static ApiResponse failure(String message, Object errors) {
        return new ApiResponse(false, message, null, errors, Map.of());
    }
}
