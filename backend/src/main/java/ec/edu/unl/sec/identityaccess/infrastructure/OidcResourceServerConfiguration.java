package ec.edu.unl.sec.identityaccess.infrastructure;

import ec.edu.unl.sec.identityaccess.application.IdentityAccessRepository;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;

/** Optional external issuer; permissions continue to come from the local scope model. */
@Configuration
@ConditionalOnProperty(name = "sec.oidc.enabled", havingValue = "true")
public class OidcResourceServerConfiguration {
    @Bean
    JwtDecoder jwtDecoder(@Value("${sec.oidc.issuer-uri}") String issuer,
            @Value("${sec.oidc.jwk-set-uri}") String jwks, @Value("${sec.oidc.audience}") String audience) {
        if (!"https".equalsIgnoreCase(URI.create(issuer).getScheme())
                || !"https".equalsIgnoreCase(URI.create(jwks).getScheme()) || audience.isBlank()) {
            throw new IllegalArgumentException("OIDC requires HTTPS issuer/JWKS and an explicit audience");
        }
        var decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();
        decoder.setJwtValidator(validator(issuer, audience));
        return decoder;
    }

    public static OAuth2TokenValidator<Jwt> validator(String issuer, String audience) {
        return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), jwt ->
                jwt.getAudience() != null && jwt.getAudience().contains(audience) && jwt.getExpiresAt() != null
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience or expiration", null)));
    }

    static Converter<Jwt, AbstractAuthenticationToken> localJwtAuthentication(IdentityAccessRepository identities) {
        return jwt -> {
            try {
                String subject = jwt.getSubject();
                if (subject == null) throw new OAuth2AuthenticationException("invalid_token");
                UUID id = UUID.fromString(subject);
                if (!id.toString().equalsIgnoreCase(subject)) throw new OAuth2AuthenticationException("invalid_token");
                var user = identities.findUser(id).filter(IdentityAccessRepository.UserRecord::active)
                        .orElseThrow(() -> new OAuth2AuthenticationException("invalid_token"));
                return new UsernamePasswordAuthenticationToken(id, null,
                        List.of(new SimpleGrantedAuthority(user.technical() ? "ROLE_TECHNICAL" : "ROLE_HUMAN")));
            } catch (IllegalArgumentException invalid) {
                throw new OAuth2AuthenticationException("invalid_token");
            }
        };
    }
}
