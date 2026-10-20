package ec.edu.unl.sec.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class PostgresTestSupport {
    private static final boolean USE_CONTAINER = Boolean.parseBoolean(System.getenv().getOrDefault("SEC_USE_TESTCONTAINERS", "false"));
    private static PostgreSQLContainer<?> container;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        if (USE_CONTAINER) {
            PostgreSQLContainer<?> postgres = container();
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        } else {
            registry.add("spring.datasource.url", () -> env("SEC_TEST_DB_URL", "jdbc:postgresql://localhost:5432/sec_unl_test"));
            registry.add("spring.datasource.username", () -> env("SEC_TEST_DB_USER", "sec_test"));
            registry.add("spring.datasource.password", () -> env("SEC_TEST_DB_PASSWORD", ""));
        }
    }

    public static String jdbcUrl() { return USE_CONTAINER ? container().getJdbcUrl() : env("SEC_TEST_DB_URL", "jdbc:postgresql://localhost:5432/sec_unl_test"); }
    public static String username() { return USE_CONTAINER ? container().getUsername() : env("SEC_TEST_DB_USER", "sec_test"); }
    public static String password() { return USE_CONTAINER ? container().getPassword() : env("SEC_TEST_DB_PASSWORD", ""); }

    private static synchronized PostgreSQLContainer<?> container() {
        if (container == null) {
            container = new PostgreSQLContainer<>("postgres:18-alpine");
            container.start();
        }
        return container;
    }

    private static String env(String name, String fallback) {
        return System.getenv().getOrDefault(name, fallback);
    }
}
