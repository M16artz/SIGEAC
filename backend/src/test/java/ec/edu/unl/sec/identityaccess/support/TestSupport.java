package ec.edu.unl.sec.identityaccess.support;

import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.identityaccess.application.IdentityAccessProperties;
import ec.edu.unl.sec.identityaccess.application.IdentityAccessRepository;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.FunctionalAssignment;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class TestSupport {
    public static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    public static final UUID CAREER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID PROCESS = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID INDICATOR = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID ADMIN = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    public static final UUID USER = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    public static final UUID ADMIN_FUNCTION = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    public static final UUID RESPONSIBLE_FUNCTION = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    public static final UUID REVIEWER_FUNCTION = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    public static final String ADMIN_PASSWORD = "Synthetic-Admin-2026";

    private TestSupport() {}

    public static MutableClock clock() {
        return new MutableClock(NOW);
    }

    public static IdentityAccessProperties properties() {
        IdentityAccessProperties properties = new IdentityAccessProperties();
        properties.setSubjectHmacSecret("synthetic-test-secret");
        properties.setBcryptStrength(4);
        return properties;
    }

    public static RecordingAuditPort audit() {
        return new RecordingAuditPort();
    }

    public static void catalogue(InMemoryIdentityAccessRepository repository) {
        repository.addFunction(new InMemoryIdentityAccessRepository.Function(
                ADMIN_FUNCTION,
                "FUNCTIONAL_ADMIN",
                "Administrador funcional",
                Set.of(
                        "ACCESS_READ_SELF",
                        "ACCESS_MANAGE_USERS",
                        "ACCESS_ASSIGN_SCOPE",
                        "ACCESS_MANAGE_EXCEPTIONS",
                        "ACCESS_READ_AUDIT"),
                true));
        repository.addFunction(new InMemoryIdentityAccessRepository.Function(
                RESPONSIBLE_FUNCTION, "RESPONSIBLE", "Responsable", Set.of("ACCESS_READ_SELF", "EVIDENCE_COLLECT"), true));
        repository.addFunction(new InMemoryIdentityAccessRepository.Function(
                REVIEWER_FUNCTION, "REVIEWER", "Revisor", Set.of("ACCESS_READ_SELF", "EVIDENCE_REVIEW"), true));
    }

    public static void admin(InMemoryIdentityAccessRepository repository) {
        catalogue(repository);
        repository.addUser(
                ADMIN,
                "admin",
                "Administrador Sintetico",
                false,
                true,
                new BCryptPasswordEncoder(4).encode(ADMIN_PASSWORD));
        repository.addAssignment(assignment(ADMIN, ADMIN_FUNCTION, CAREER, null, null));
    }

    public static void user(InMemoryIdentityAccessRepository repository) {
        repository.addUser(
                USER, "responsable", "Responsable Sintetico", false, true,
                new BCryptPasswordEncoder(4).encode(ADMIN_PASSWORD));
    }

    public static FunctionalAssignment assignment(
            UUID identityId, UUID functionId, UUID careerId, UUID processId, UUID indicatorId) {
        return new FunctionalAssignment(
                UUID.randomUUID(),
                identityId,
                functionId,
                new AuthorizationScope(careerId, processId, indicatorId),
                NOW.minus(1, ChronoUnit.DAYS),
                NOW.plus(365, ChronoUnit.DAYS),
                ADMIN,
                null,
                0);
    }

    public static final class MutableClock extends Clock {
        private Instant instant;

        public MutableClock(Instant instant) {
            this.instant = instant;
        }

        public void advance(java.time.Duration duration) {
            instant = instant.plus(duration);
        }

        public void set(Instant value) {
            instant = value;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    public static final class RecordingAuditPort implements AuditPort {
        public final List<AuditEventCommand> accepted = new ArrayList<>();
        public final List<AuditEventCommand> rejected = new ArrayList<>();

        @Override
        public UUID record(AuditEventCommand command) {
            accepted.add(command);
            return UUID.randomUUID();
        }

        @Override
        public UUID recordRejection(AuditEventCommand command) {
            rejected.add(command);
            return UUID.randomUUID();
        }
    }

    public static IdentityAccessRepository repository(InMemoryIdentityAccessRepository repository) {
        return repository;
    }
}
