package ec.edu.unl.sec.archive.application;

import org.springframework.stereotype.Service;

@Service
public class IntegralClosureService {
    public Preview preview(String processId) {
        return new Preview(java.util.List.of(), true);
    }
    public void close(String processId) {}
    public record Preview(java.util.List<String> blockers, boolean allowed) {}
}
