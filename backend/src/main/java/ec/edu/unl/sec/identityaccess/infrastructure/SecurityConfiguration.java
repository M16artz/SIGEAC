package ec.edu.unl.sec.identityaccess.infrastructure;

import ec.edu.unl.sec.identityaccess.application.IdentityAccessProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, SessionAuthenticationFilter sessionFilter, AccessProblemWriter problems,
            org.springframework.beans.factory.ObjectProvider<org.springframework.security.oauth2.jwt.JwtDecoder> decoders,
            ec.edu.unl.sec.identityaccess.application.IdentityAccessRepository identities)
            throws Exception {
        http.cors(org.springframework.security.config.Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/sessions")
                        .permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info", "/error")
                        .permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, exception) -> problems.write(
                                request,
                                response,
                                HttpStatus.UNAUTHORIZED,
                                "AUTHENTICATION_REQUIRED",
                                "Se requiere una sesion vigente."))
                        .accessDeniedHandler((request, response, exception) -> problems.write(
                                request,
                                response,
                                HttpStatus.FORBIDDEN,
                                "ACCESS_DENIED",
                                "La operacion no esta permitida en el ambito actual.")))
                .addFilterBefore(sessionFilter, UsernamePasswordAuthenticationFilter.class);
        var decoder = decoders.getIfAvailable();
        if (decoder != null) {
            http.oauth2ResourceServer(resource -> resource
                    .jwt(jwt -> jwt.decoder(decoder).jwtAuthenticationConverter(OidcResourceServerConfiguration.localJwtAuthentication(identities)))
                    .authenticationEntryPoint((request, response, exception) -> {
                        response.setHeader("WWW-Authenticate", "Bearer");
                        problems.write(request, response, HttpStatus.UNAUTHORIZED, "INVALID_BEARER_TOKEN", "Se requiere un token de acceso válido.");
                    }));
        }
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder(IdentityAccessProperties properties) {
        return new BCryptPasswordEncoder(properties.getBcryptStrength());
    }

    @Bean
    org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource(
            @org.springframework.beans.factory.annotation.Value("${sec.http.allowed-origins:}") String origins) {
        var configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOrigins(java.util.Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        if (configuration.getAllowedOrigins().contains("*")) throw new IllegalArgumentException("Credentialed CORS requires explicit origins");
        configuration.setAllowCredentials(true);
        configuration.setAllowedMethods(java.util.List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("Content-Type", "Authorization", "X-CSRF-TOKEN", "If-Match", "X-Correlation-ID"));
        configuration.setExposedHeaders(java.util.List.of("ETag", "Location", "X-Correlation-ID", "Retry-After"));
        var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    org.springframework.web.filter.CorsFilter corsFilter(@org.springframework.beans.factory.annotation.Qualifier("corsConfigurationSource") org.springframework.web.cors.CorsConfigurationSource source,
            com.fasterxml.jackson.databind.ObjectMapper mapper) {
        var filter = new org.springframework.web.filter.CorsFilter(source);
        filter.setCorsProcessor(new org.springframework.web.cors.DefaultCorsProcessor() {
            @Override
            protected void rejectRequest(org.springframework.http.server.ServerHttpResponse response) throws java.io.IOException {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                response.getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                mapper.writeValue(response.getBody(), ec.edu.unl.sec.shared.api.ApiResponse.failure(
                        "Origen o preflight CORS no permitido.", java.util.Map.of(
                                "type", "urn:sec-unl:problem:cors-denied", "title", "Forbidden", "status", 403,
                                "detail", "Origen o preflight CORS no permitido.", "instance", "about:blank",
                                "code", "CORS_DENIED", "details", java.util.Map.of(), "correlationId",
                                java.util.Optional.ofNullable(response.getHeaders().getFirst("X-Correlation-ID")).orElse("unavailable"))));
                response.flush();
            }
        });
        return filter;
    }
}
