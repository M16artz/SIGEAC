package ec.edu.unl.sec.evaluationmodel.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.evaluationmodel.domain.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class ConfiguredModelFixtures {
    private ConfiguredModelFixtures() {}

    public static Path path(String name) {
        return Path.of(System.getProperty("basedir", "."), "..", "tests", "fixtures", "models", name);
    }

    public static Manifest manifest() {
        try {
            return new ObjectMapper().findAndRegisterModules().readValue(path("five-model-configurations.json").toFile(), Manifest.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read synthetic model fixtures", exception);
        }
    }

    public static Fixture first() { return manifest().models().getFirst(); }

    public static ValuationRule withStatus(ValuationRule rule, ValuationRule.Status status) {
        return new ValuationRule(rule.id(), rule.indicatorNodeId(), rule.ruleType(), rule.methodDefinition(),
                rule.formulaDefinition(), rule.strategyKey(), rule.strategyVersion(), rule.scaleId(), rule.variables(),
                rule.thresholds(), rule.missingDataPolicy(), status);
    }

    public record Manifest(String schemaVersion, boolean synthetic, List<Fixture> models) {}
    public record Fixture(String key, String name, boolean synthetic, String source,
            List<ModelNode> nodes, ModelConfiguration configuration) {}
}
