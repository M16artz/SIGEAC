package ec.edu.unl.sec.identityaccess.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

class SessionCookieTest {
    @Test void productionRequiresTlsAndSecureCookiesIncludingLogout() throws Exception {
        var prod = new YamlPropertySourceLoader().load("prod",new ClassPathResource("application-prod.yml")).getFirst();
        assertThat(prod.getProperty("sec.identity-access.cookie-secure")).isEqualTo(true);
        assertThat(prod.getProperty("sec.http.require-https")).isEqualTo(true);
        assertThat(prod.getProperty("server.ssl.enabled")).isEqualTo(true);
        assertThat(SessionCookie.create("dummy",Duration.ofMinutes(30),true).toString())
                .contains("Secure","HttpOnly","SameSite=Lax");
        assertThat(SessionCookie.clear(true).toString()).contains("Secure","Max-Age=0");
    }
}
