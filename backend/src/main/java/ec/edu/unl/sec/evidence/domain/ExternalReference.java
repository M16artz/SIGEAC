package ec.edu.unl.sec.evidence.domain;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

/** A declaration only: URI validation never establishes reachability or integrity. */
public record ExternalReference(UUID id, UUID versionId, String uri, String description, int position) {
    public ExternalReference {
        Objects.requireNonNull(id);
        Objects.requireNonNull(versionId);
        if (uri == null || uri.length() > 2048 || !URI.create(uri).isAbsolute()) {
            throw new IllegalArgumentException("Absolute URI of at most 2048 characters required");
        }
        var parsed = URI.create(uri);
        boolean web = ("http".equalsIgnoreCase(parsed.getScheme()) || "https".equalsIgnoreCase(parsed.getScheme()))
                && parsed.getHost() != null && parsed.getUserInfo() == null;
        boolean identifier = "urn".equalsIgnoreCase(parsed.getScheme()) && parsed.isOpaque()
                && parsed.getRawSchemeSpecificPart().contains(":");
        if (!web && !identifier) throw new IllegalArgumentException("HTTP(S) link or namespaced URN required");
        if (description == null || description.isBlank() || description.strip().length() > 1000 || position < 1) {
            throw new IllegalArgumentException("Invalid external reference");
        }
        description = description.strip();
    }
}
