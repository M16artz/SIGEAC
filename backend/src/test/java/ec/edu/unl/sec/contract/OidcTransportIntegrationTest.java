package ec.edu.unl.sec.contract;

import static org.assertj.core.api.Assertions.assertThat;
import ec.edu.unl.sec.identityaccess.application.IdentityAccessRepository;
import java.net.URI;
import java.net.http.*;
import java.security.*;
import java.security.interfaces.*;
import java.time.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.test.context.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("sqlite-test")
@Import(OidcTransportIntegrationTest.Keys.class)
class OidcTransportIntegrationTest {
    private static final String DATABASE = "oidc-transport-" + UUID.randomUUID();
    private static final KeyPair KEY = key();
    @org.junit.jupiter.api.io.TempDir static java.nio.file.Path files;
    @Autowired IdentityAccessRepository identities;
    @Value("${local.server.port}") int port;

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:file:" + DATABASE + "?mode=memory&cache=shared");
        registry.add("sec.files.root", () -> files.toString());
        registry.add("sec.oidc.enabled", () -> "true");
        registry.add("sec.oidc.issuer-uri", () -> "https://issuer.example");
        registry.add("sec.oidc.jwk-set-uri", () -> "https://issuer.example/keys");
        registry.add("sec.oidc.audience", () -> "sec-api");
    }

    @Test void acceptsSignedBearerWithoutCookieAndKeepsLocalPermissionsAndDeactivation() throws Exception {
        var client = HttpClient.newHttpClient();
        var admin = identities.findLogin("admin").orElseThrow().identityId();
        var reader = identities.findLogin("responsable").orElseThrow().identityId();
        String bearer = token(KEY, admin, "https://issuer.example", "sec-api", Instant.now().plusSeconds(300));
        assertThat(send(client, "GET", "/me", bearer).statusCode()).isEqualTo(200);
        assertThat(send(client, "POST", "/academic-units", bearer).statusCode()).isEqualTo(201);
        // A synthetic ADMIN claim cannot turn a local responsible identity into an administrator.
        String readerToken = token(KEY, reader, "https://issuer.example", "sec-api", Instant.now().plusSeconds(300));
        assertThat(send(client, "POST", "/academic-units", readerToken).statusCode()).isEqualTo(403);
        for (String invalid : List.of(
                token(key(), admin, "https://issuer.example", "sec-api", Instant.now().plusSeconds(300)),
                token(KEY, admin, "https://other.example", "sec-api", Instant.now().plusSeconds(300)),
                token(KEY, null, "https://issuer.example", "sec-api", Instant.now().plusSeconds(300)),
                token(KEY, admin, "https://issuer.example", null, Instant.now().plusSeconds(300)),
                token(KEY, admin, "https://issuer.example", "another-api", Instant.now().plusSeconds(300)),
                token(KEY, admin, "https://issuer.example", "sec-api", Instant.now().minusSeconds(600)))) {
            var response = send(client, "GET", "/me", invalid);
            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.headers().firstValue("WWW-Authenticate")).contains("Bearer");
            assertThat(new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.body()).path("success").asBoolean()).isFalse();
        }
        assertThat(send(client, "DELETE", "/sessions", bearer).statusCode()).isEqualTo(400);
        identities.deactivateIdentity(admin, identities.findUser(admin).orElseThrow().revision(), Instant.now());
        assertThat(send(client, "GET", "/me", bearer).statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> send(HttpClient client, String method, String path, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1" + path))
                .timeout(Duration.ofSeconds(15)).header("Authorization", "Bearer " + token).header("Content-Type", "application/json");
        builder.method(method, method.equals("POST") ? HttpRequest.BodyPublishers.ofString(
                "{\"code\":\"OIDC-" + UUID.randomUUID() + "\",\"name\":\"Synthetic unit\"}") : HttpRequest.BodyPublishers.noBody());
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    private static KeyPair key() {
        try { var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048); return generator.generateKeyPair(); }
        catch (GeneralSecurityException exception) { throw new IllegalStateException(exception); }
    }
    private static String token(KeyPair key, UUID subject, String issuer, String audience, Instant expires) {
        var jwk = new com.nimbusds.jose.jwk.RSAKey.Builder((RSAPublicKey) key.getPublic()).privateKey((RSAPrivateKey) key.getPrivate()).build();
        var encoder = new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableJWKSet<>(new com.nimbusds.jose.jwk.JWKSet(jwk)));
        var claims = JwtClaimsSet.builder().issuer(issuer)
                .issuedAt(Instant.now().minusSeconds(1200)).expiresAt(expires).claim("roles", List.of("ADMIN"));
        if (subject != null) claims.subject(subject.toString());
        if (audience != null) claims.audience(List.of(audience));
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256).build(), claims.build())).getTokenValue();
    }
    @TestConfiguration static class Keys {
        @Bean @Primary JwtDecoder testDecoder() {
            var decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) KEY.getPublic()).build();
            decoder.setJwtValidator(ec.edu.unl.sec.identityaccess.infrastructure.OidcResourceServerConfiguration
                    .validator("https://issuer.example", "sec-api"));
            return decoder;
        }
    }
}
