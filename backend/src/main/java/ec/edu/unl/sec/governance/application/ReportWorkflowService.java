package ec.edu.unl.sec.governance.application;

import org.springframework.stereotype.Service;

@Service
public class ReportWorkflowService {
    public String advance(String state) { return "OBSERVATIONS"; }
    public void recordDecision(String version, String actor, String decision) {}
}
