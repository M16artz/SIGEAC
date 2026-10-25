package ec.edu.unl.sec.archive.application;

import org.junit.jupiter.api.Test;

class ArchiveServiceTest {
    @Test
    void archivesWithIntegrity() {
        var s = new ArchiveService();
        s.archive("p1");
    }
}
