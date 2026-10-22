package ec.edu.unl.sec.shared.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.nio.ByteBuffer;
import java.util.UUID;

public final class JdbcValueSupport {
    private JdbcValueSupport() {}

    public static UUID uuid(ResultSet result, int index) throws SQLException {
        Object value = result.getObject(index);
        if (value == null) return null;
        if (value instanceof UUID uuid) return uuid;
        if (value instanceof byte[] bytes && bytes.length == 16) {
            var buffer = ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        }
        return UUID.fromString(value.toString());
    }

    public static LocalDate localDate(ResultSet result, int index) throws SQLException {
        Object value = result.getObject(index);
        if (value == null) return null;
        if (value instanceof LocalDate date) return date;
        if (value instanceof java.sql.Date date) return date.toLocalDate();
        return LocalDate.parse(value.toString());
    }

    public static Instant instant(ResultSet result, int index) throws SQLException {
        Object value = result.getObject(index);
        if (value == null) return null;
        if (value instanceof Timestamp timestamp) return timestamp.toInstant();
        if (value instanceof Instant instant) return instant;
        if (value instanceof OffsetDateTime offset) return offset.toInstant();
        if (value instanceof Number epochMillis) return Instant.ofEpochMilli(epochMillis.longValue());
        String text = value.toString();
        if (text.matches("-?[0-9]+")) return Instant.ofEpochMilli(Long.parseLong(text));
        try {
            return Instant.parse(text);
        } catch (java.time.format.DateTimeParseException ignored) {
            try {
                return Timestamp.valueOf(text).toInstant();
            } catch (IllegalArgumentException invalidTimestamp) {
                throw new SQLException("Unsupported timestamp value: " + text, invalidTimestamp);
            }
        }
    }
}
