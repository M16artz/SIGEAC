package ec.edu.unl.sec.identityaccess.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bounded per-IP limiter for one instance; the gateway enforces the shared external limit. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class HttpBoundaryFilter extends OncePerRequestFilter {
    private static final Set<String> SECRET_PARAMETERS = Set.of("password", "token", "access_token", "api_key", "client_secret", "authorization");
    private final AccessProblemWriter problems;
    private final boolean requireHttps;
    private final int requestsPerMinute;
    private final int maxClients;
    private final Clock clock;
    private final Map<String, Window> clients = new HashMap<>();
    private long cleanupAt;

    @org.springframework.beans.factory.annotation.Autowired
    public HttpBoundaryFilter(AccessProblemWriter problems,
            @Value("${sec.http.require-https:false}") boolean requireHttps,
            @Value("${sec.http.requests-per-minute:600}") int requestsPerMinute,
            @Value("${sec.http.max-rate-limit-clients:10000}") int maxClients) {
        this(problems, requireHttps, requestsPerMinute, maxClients, Clock.systemUTC());
    }

    HttpBoundaryFilter(AccessProblemWriter problems, boolean requireHttps, int limit, int maxClients, Clock clock) {
        if (limit < 1 || maxClients < 1) throw new IllegalArgumentException("HTTP rate limits must be positive");
        this.problems = problems; this.requireHttps = requireHttps; this.requestsPerMinute = limit;
        this.maxClients = maxClients; this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        if (requireHttps && !request.isSecure()) {
            response.setHeader("Upgrade", "TLS/1.2");
            problems.write(request, response, HttpStatus.UPGRADE_REQUIRED, "HTTPS_REQUIRED", "La API requiere HTTPS.");
            return;
        }
        if ("TRACE".equals(request.getMethod())) {
            problems.write(request, response, HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "El método no está permitido.");
            return;
        }
        String query = request.getQueryString();
        if (query != null) {
            try {
                for (String pair : query.split("&")) {
                    String[] parts = pair.split("=", 2);
                    String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8).toLowerCase(java.util.Locale.ROOT);
                    if (SECRET_PARAMETERS.contains(key)) throw new IllegalArgumentException();
                    if (key.equals("page") || key.equals("size")) {
                        int value = Integer.parseInt(parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "");
                        if ((key.equals("page") && (value < 0 || value > Integer.MAX_VALUE / 100))
                                || (key.equals("size") && (value < 1 || value > 100))) throw new IllegalArgumentException();
                    }
                }
            } catch (IllegalArgumentException invalid) {
                problems.write(request, response, HttpStatus.BAD_REQUEST, "INVALID_QUERY", "Consulta inválida: no envíe secretos en la URI y use page >= 0 y size entre 1 y 100.");
                return;
            }
        }
        long retry = reserve(request.getRemoteAddr()); // Never trust an arbitrary X-Forwarded-For header.
        if (retry > 0) {
            response.setHeader("Retry-After", Long.toString(retry));
            problems.write(request, response, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Demasiadas solicitudes; vuelva a intentar después.");
            return;
        }
        chain.doFilter(request, response);
    }

    private synchronized long reserve(String client) {
        long now = clock.millis();
        if (now >= cleanupAt) {
            clients.values().removeIf(window -> now >= window.expires);
            cleanupAt = now + 60_000;
        }
        Window window = clients.get(client);
        if (window == null || now >= window.expires) {
            if (window == null && clients.size() >= maxClients) return 60;
            window = new Window(now + 60_000);
            clients.put(client, window);
        }
        if (window.count >= requestsPerMinute) return Math.max(1, (window.expires - now + 999) / 1000);
        window.count++;
        return 0;
    }

    private static final class Window {
        final long expires;
        int count;
        Window(long expires) { this.expires = expires; }
    }
}
