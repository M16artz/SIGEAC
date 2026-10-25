package ec.edu.unl.sec.archive.application;

import org.junit.jupiter.api.Test;

class ClosureReopeningPolicyTest {
    @Test
    void handlesBlockersAndReopening() {
        var s = new IntegralClosureService();
        s.preview("p1");
        s.close("p1");
    }
}
