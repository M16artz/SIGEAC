package ec.edu.unl.sec.identityaccess.infrastructure;

import ec.edu.unl.sec.identityaccess.application.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {
    public static final String CSRF_ATTRIBUTE = SessionAuthenticationFilter.class.getName() + ".csrf";

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final SessionService sessions;
    private final AccessProblemWriter problems;

    public SessionAuthenticationFilter(SessionService sessions, AccessProblemWriter problems) {
        this.sessions = sessions;
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var token = SessionCookie.read(request);
        boolean authenticated = false;
        if (token.isPresent()) {
            var session = sessions.authenticateForRequest(token.get());
            if (session.isPresent()) {
                var authenticatedSession = session.get();
                var authorities = List.of(new SimpleGrantedAuthority(
                        authenticatedSession.technical() ? "ROLE_TECHNICAL" : "ROLE_HUMAN"));
                var authentication = new UsernamePasswordAuthenticationToken(
                        authenticatedSession.identityId(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                request.setAttribute(CSRF_ATTRIBUTE, authenticatedSession.csrfHash());
                authenticated = true;
            }
        }

        if (authenticated && requiresCsrf(request)) {
            String provided = request.getHeader(SessionCookie.csrfHeader());
            Object expected = request.getAttribute(CSRF_ATTRIBUTE);
            if (provided == null
                    || expected == null
                    || !MessageDigest.isEqual(
                            SessionService.sha256Hex(provided).getBytes(StandardCharsets.US_ASCII),
                            expected.toString().getBytes(StandardCharsets.US_ASCII))) {
                problems.write(
                        request,
                        response,
                        HttpStatus.FORBIDDEN,
                        "CSRF_TOKEN_INVALID",
                        "La solicitud requiere un token CSRF vigente.");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private static boolean requiresCsrf(HttpServletRequest request) {
        if (SAFE_METHODS.contains(request.getMethod())) return false;
        return !(("POST".equals(request.getMethod())) && "/sessions".equals(path(request)));
    }

    private static String path(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && uri.startsWith(context)) {
            return uri.substring(context.length());
        }
        return uri;
    }
}
