package ec.edu.unl.sec.identityaccess.infrastructure;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Optional;
import org.springframework.http.ResponseCookie;

public final class SessionCookie {
    public static final String NAME = "SEC_SESSION";
    private static final String CSRF_HEADER = "X-CSRF-TOKEN";

    private SessionCookie() {}

    public static Optional<String> read(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();
        for (Cookie cookie : request.getCookies()) {
            if (NAME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    public static String csrfHeader() {
        return CSRF_HEADER;
    }

    public static ResponseCookie create(String token, Duration maxAge, boolean secure) {
        return ResponseCookie.from(NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    public static ResponseCookie clear(boolean secure) {
        return ResponseCookie.from(NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }
}
