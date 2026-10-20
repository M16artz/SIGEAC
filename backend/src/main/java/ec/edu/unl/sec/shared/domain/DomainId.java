package ec.edu.unl.sec.shared.domain;

import java.util.Objects;
import java.util.UUID;

public record DomainId(UUID value) {
    public DomainId {
        Objects.requireNonNull(value, "value is required");
    }

    public static DomainId generate() {
        return new DomainId(UUID.randomUUID());
    }

    public static DomainId parse(String value) {
        return new DomainId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
