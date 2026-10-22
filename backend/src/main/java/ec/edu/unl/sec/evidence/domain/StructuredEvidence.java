package ec.edu.unl.sec.evidence.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Typed snapshot for structured-data and institutional-reference source
 * modalities (FR-069 / AC-X16): value, optional unit and recorded period.
 * Files are not required for these modalities; snapshots share the version
 * immutability that protects files after submission.
 */
public record StructuredEvidence(UUID id, UUID versionId, UUID requiredSourceId, Kind kind, TypedValue value,
        String unit, LocalDate periodStart, LocalDate periodEnd, String reference, UUID createdBy, Instant createdAt) {
    public StructuredEvidence {
        id = id == null ? UUID.randomUUID() : id;
        Objects.requireNonNull(versionId, "version required");
        Objects.requireNonNull(requiredSourceId, "required source required");
        Objects.requireNonNull(kind, "kind required");
        Objects.requireNonNull(value, "typed value required");
        unit = unit == null || unit.isBlank() ? null : unit.strip();
        if (unit != null && unit.length() > 100) throw new IllegalArgumentException("unit too long");
        Objects.requireNonNull(periodStart, "period start required");
        Objects.requireNonNull(periodEnd, "period end required");
        if (periodStart.isAfter(periodEnd)) throw new IllegalArgumentException("invalid period");
        reference = reference == null || reference.isBlank() ? null : reference.strip();
        if (reference != null && reference.length() > 2000) throw new IllegalArgumentException("reference too long");
        Objects.requireNonNull(createdBy, "author required");
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public enum Kind {
        STRUCTURED_DATA, INSTITUTIONAL_REFERENCE
    }

    public enum ValueType {
        NUMBER, STRING, BOOLEAN
    }

    public record TypedValue(ValueType type, BigDecimal number, String text, Boolean flag) {
        public TypedValue {
            Objects.requireNonNull(type, "value type required");
        }

        public static TypedValue number(BigDecimal number) {
            Objects.requireNonNull(number, "number required");
            return new TypedValue(ValueType.NUMBER, number, null, null);
        }

        public static TypedValue text(String text) {
            Objects.requireNonNull(text, "text required");
            return new TypedValue(ValueType.STRING, null, text, null);
        }

        public static TypedValue flag(Boolean flag) {
            Objects.requireNonNull(flag, "flag required");
            return new TypedValue(ValueType.BOOLEAN, null, null, flag);
        }

        public Object value() {
            return switch (type) {
                case NUMBER -> number();
                case STRING -> (Object) text();
                case BOOLEAN -> Boolean.valueOf(flag().booleanValue());
            };
        }
    }
}
