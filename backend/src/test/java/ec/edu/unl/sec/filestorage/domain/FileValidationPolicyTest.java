package ec.edu.unl.sec.filestorage.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class FileValidationPolicyTest {
    public static FileValidationPolicy policy(boolean allowEmpty) {
        return new FileValidationPolicy(UUID.randomUUID(), "synthetic-v1", 26_214_400,
                allowEmpty, Set.of("text/plain", "application/pdf", "application/octet-stream"),
                Map.of("text/plain", Set.of("text/plain"), "application/pdf", Set.of("application/pdf"),
                        "application/octet-stream", Set.of("text/plain", "application/octet-stream")));
    }

    @Test void enforcesExactLimitAndConfigurableEmptyPolicy() {
        policy(false).validate(26_214_400, "text/plain", "text/plain");
        assertThatThrownBy(() -> policy(false).validate(26_214_401, "text/plain", "text/plain"))
                .isInstanceOf(FileValidationException.class).hasMessage("FILE_TOO_LARGE");
        assertThatThrownBy(() -> policy(false).validate(0, "text/plain", "text/plain"))
                .hasMessage("FILE_EMPTY");
        policy(true).validate(0, "application/octet-stream", "application/octet-stream");
        assertThatThrownBy(() -> policy(true).validate(-1, "text/plain", "text/plain"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void requiresBothDetectedAllowlistAndExplicitCompatibility() {
        assertThatThrownBy(() -> policy(false).validate(10, "text/plain", "application/x-executable"))
                .hasMessage("FILE_TYPE_NOT_ALLOWED");
        assertThatThrownBy(() -> policy(false).validate(10, "application/pdf", "text/plain"))
                .hasMessage("FILE_TYPE_MISMATCH");
        policy(false).validate(10, " Text/Plain; charset=UTF-8", "text/plain");
        policy(false).validate(10, "application/octet-stream", "text/plain");
        assertThatThrownBy(() -> policy(false).validate(10, null, "text/plain"))
                .hasMessage("FILE_TYPE_MISMATCH");
    }

    @Test void cannotConfigureBeyondTheDomainCeiling() {
        assertThatThrownBy(() -> new FileValidationPolicy(UUID.randomUUID(), "invalid", 26_214_401,
                false, Set.of("text/plain"), Map.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
