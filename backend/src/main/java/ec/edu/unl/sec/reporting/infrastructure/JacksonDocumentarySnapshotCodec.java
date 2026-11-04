package ec.edu.unl.sec.reporting.infrastructure;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ec.edu.unl.sec.reporting.application.DocumentarySnapshotCodec;
import ec.edu.unl.sec.reporting.application.ReportingProblemException;
import ec.edu.unl.sec.reporting.domain.DocumentarySnapshot;
import ec.edu.unl.sec.reporting.domain.DocumentaryClosure;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class JacksonDocumentarySnapshotCodec implements DocumentarySnapshotCodec {
    private final ObjectMapper json;
    public JacksonDocumentarySnapshotCodec(ObjectMapper json) {
        this.json=json.copy().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY,true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,true);
    }

    @Override
    public DocumentarySnapshot seal(DocumentarySnapshot.Content content) {
        try {
            var payload=json.writeValueAsString(content);
            return new DocumentarySnapshot(content,payload,digest(payload));
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException("Cannot encode documentary snapshot",exception);
        }
    }

    @Override
    public DocumentarySnapshot read(String payload,String sha256) {
        try {
            if (payload==null || sha256==null || !MessageDigest.isEqual(digest(payload).getBytes(StandardCharsets.US_ASCII),
                    sha256.getBytes(StandardCharsets.US_ASCII))) throw integrity();
            var content=json.readValue(payload,DocumentarySnapshot.Content.class);
            if (content==null) throw integrity();
            return new DocumentarySnapshot(content,payload,sha256);
        } catch (com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException exception) {
            throw integrity();
        }
    }

    public static ReportingProblemException integrity() {
        return new ReportingProblemException(ReportingProblemException.Kind.CONFLICT,"SNAPSHOT_INTEGRITY_FAILED",
                "No fue posible verificar la integridad del corte documental.");
    }

    @Override
    public DocumentaryClosure sealClosure(DocumentaryClosure.Content content) {
        try {
            var payload=json.writeValueAsString(content);
            return new DocumentaryClosure(content,payload,digest(payload));
        } catch (com.fasterxml.jackson.core.JsonProcessingException failure) {
            throw new IllegalStateException("Cannot encode documentary closure",failure);
        }
    }

    @Override
    public DocumentaryClosure readClosure(String payload,String sha256) {
        try {
            if (payload==null || sha256==null || !MessageDigest.isEqual(digest(payload).getBytes(StandardCharsets.US_ASCII),
                    sha256.getBytes(StandardCharsets.US_ASCII))) throw integrity();
            var content=json.readValue(payload,DocumentaryClosure.Content.class);
            if (content==null) throw integrity();
            return new DocumentaryClosure(content,payload,sha256);
        } catch (com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException invalid) { throw integrity(); }
    }

    private static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
