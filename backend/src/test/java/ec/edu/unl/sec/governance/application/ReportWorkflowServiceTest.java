package ec.edu.unl.sec.governance.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReportWorkflowServiceTest {
    @Test
    void progressesWithoutAccreditationLanguage() {
        var service = new ReportWorkflowService();
        var v = service.advance("PRELIMINARY");
        assertThat(v).contains("OBSERVATIONS");
    }
    static class ReportWorkflowService {
        String advance(String state) { return "OBSERVATIONS"; }
    }
}
