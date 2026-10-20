package ec.edu.unl.sec.shared.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@FunctionalInterface
public interface DomainClock {
    Instant now();

    static DomainClock systemUtc() {
        Clock clock = Clock.system(ZoneOffset.UTC);
        return clock::instant;
    }

    static DomainClock fixed(Instant instant) {
        return () -> instant;
    }
}
