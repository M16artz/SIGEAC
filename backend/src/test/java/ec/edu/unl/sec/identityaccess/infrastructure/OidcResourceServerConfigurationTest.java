package ec.edu.unl.sec.identityaccess.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import ec.edu.unl.sec.identityaccess.application.IdentityAccessRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

class OidcResourceServerConfigurationTest {
    @Test void verifiesSignaturesAndRejectsAnotherSigningKey() throws Exception {
        var generator = java.security.KeyPairGenerator.getInstance("RSA"); generator.initialize(2048);
        var key = generator.generateKeyPair();
        var decoder = org.springframework.security.oauth2.jwt.NimbusJwtDecoder.withPublicKey((java.security.interfaces.RSAPublicKey) key.getPublic()).build();
        decoder.setJwtValidator(OidcResourceServerConfiguration.validator("https://issuer.example", "sec-api"));
        String valid = signedToken(key);
        assertThat(decoder.decode(valid).getAudience()).contains("sec-api");
        String forged = signedToken(generator.generateKeyPair());
        assertThatThrownBy(() -> decoder.decode(forged)).isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
    }

    private String signedToken(java.security.KeyPair key) {
        var jwk = new com.nimbusds.jose.jwk.RSAKey.Builder((java.security.interfaces.RSAPublicKey) key.getPublic())
                .privateKey((java.security.interfaces.RSAPrivateKey) key.getPrivate()).build();
        var encoder = new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(
                new com.nimbusds.jose.jwk.source.ImmutableJWKSet<>(new com.nimbusds.jose.jwk.JWKSet(jwk)));
        var claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder().issuer("https://issuer.example")
                .audience(java.util.List.of("sec-api")).subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
        return encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
    }
    @Test void rejectsWrongIssuerAudienceExpiredOrMissingExpiry() {
        var validator = OidcResourceServerConfiguration.validator("https://issuer.example", "sec-api");
        assertThat(validator.validate(token("https://issuer.example", "sec-api", Instant.now().plusSeconds(300))).hasErrors()).isFalse();
        assertThat(validator.validate(token("https://other.example", "sec-api", Instant.now().plusSeconds(300))).hasErrors()).isTrue();
        assertThat(validator.validate(token("https://issuer.example", "other", Instant.now().plusSeconds(300))).hasErrors()).isTrue();
        assertThat(validator.validate(token("https://issuer.example", "sec-api", Instant.now().minusSeconds(600))).hasErrors()).isTrue();
        var noExpiry = Jwt.withTokenValue("synthetic").header("alg", "RS256").issuer("https://issuer.example").audience(java.util.List.of("sec-api")).subject(UUID.randomUUID().toString()).build();
        assertThat(validator.validate(noExpiry).hasErrors()).isTrue();
        var noAudience = Jwt.withTokenValue("synthetic").header("alg", "RS256").issuer("https://issuer.example")
                .expiresAt(Instant.now().plusSeconds(300)).subject(UUID.randomUUID().toString()).build();
        assertThat(validator.validate(noAudience).hasErrors()).isTrue();
    }
    @Test void mapsOnlyEnabledLocalIdentitiesWithoutTrustingTokenRoles() {
        var repository = mock(IdentityAccessRepository.class); UUID id = UUID.randomUUID();
        var jwt = Jwt.withTokenValue("synthetic").header("alg", "RS256").subject(id.toString()).claim("roles", java.util.List.of("ADMIN")).build();
        var converter = OidcResourceServerConfiguration.localJwtAuthentication(repository);
        when(repository.findUser(id)).thenReturn(Optional.of(new IdentityAccessRepository.UserRecord(id, "synthetic", "Synthetic", false, true, 0)));
        var authentication = converter.convert(jwt);
        assertThat(authentication.getPrincipal()).isEqualTo(id);
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_HUMAN");
        when(repository.findUser(id)).thenReturn(Optional.of(new IdentityAccessRepository.UserRecord(id, "synthetic", "Synthetic", false, false, 0)));
        assertThatThrownBy(() -> converter.convert(jwt)).isInstanceOf(OAuth2AuthenticationException.class);
        var noSubject = Jwt.withTokenValue("synthetic").header("alg", "RS256").issuer("https://issuer.example").build();
        assertThatThrownBy(() -> converter.convert(noSubject)).isInstanceOf(OAuth2AuthenticationException.class);
    }
    private Jwt token(String issuer, String audience, Instant expiration) {
        return Jwt.withTokenValue("synthetic").header("alg", "RS256").issuer(issuer).audience(java.util.List.of(audience))
                .issuedAt(Instant.now().minusSeconds(1200)).expiresAt(expiration).subject(UUID.randomUUID().toString()).build();
    }
}
