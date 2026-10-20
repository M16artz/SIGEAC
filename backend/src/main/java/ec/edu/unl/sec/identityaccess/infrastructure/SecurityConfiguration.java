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
            HttpSecurity http, SessionAuthenticationFilter sessionFilter, AccessProblemWriter problems)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/sessions")
                        .permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info", "/error")
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
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder(IdentityAccessProperties properties) {
        return new BCryptPasswordEncoder(properties.getBcryptStrength());
    }
}
