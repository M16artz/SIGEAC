package ec.edu.unl.sec.academicevaluation.application;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class VariableValueService {
    private final Map<UUID, Map<String, String>> values = new HashMap<>();

    public void register(UUID indicatorId, String key, String value) {
        values.computeIfAbsent(indicatorId, k -> new HashMap<>()).put(key, value);
    }

    public Map<String, String> get(UUID indicatorId) {
        return Map.copyOf(values.getOrDefault(indicatorId, Map.of()));
    }
}
