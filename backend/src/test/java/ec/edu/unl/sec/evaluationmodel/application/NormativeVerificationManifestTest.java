package ec.edu.unl.sec.evaluationmodel.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.evaluationmodel.support.ConfiguredModelFixtures;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NormativeVerificationManifestTest {
    @Test void verifiesEverySyntheticSourceWithoutClaimingInstitutionalValidity() throws Exception {
        var json = new ObjectMapper();
        JsonNode manifest = json.readTree(ConfiguredModelFixtures.path("normative-verification.json").toFile());
        var configuration = ConfiguredModelFixtures.manifest();
        assertThat(manifest.path("synthetic").asBoolean()).isTrue();
        assertThat(manifest.path("institutionalValidity").asBoolean()).isFalse();
        assertThat(manifest.path("disclaimer").asText()).contains("PENDING_VALIDATION");

        Set<String> modelKeys = new HashSet<>();
        configuration.models().forEach(model -> modelKeys.add(model.key()));
        Set<String> manifestedKeys = new HashSet<>();
        Set<String> decisions = new HashSet<>();
        for (JsonNode source : manifest.path("sources")) {
            assertThat(source.path("verificationStatus").asText()).isEqualTo("PENDING_VALIDATION");
            assertThat(source.path("pages").isNull()).isTrue();
            assertThat(source.path("documentSha256").isNull()).isTrue();
            assertThat(source.path("editionVerified").isNull()).isTrue();
            assertThat(source.path("unresolvedApplicability").asBoolean()).isTrue();
            assertThat(source.path("statesApplicability").asBoolean()).isFalse();
            assertThat(source.path("institutionalOwner").asText()).isNotBlank();
            assertThat(source.path("blockedReason").asText()).isNotBlank();
            source.path("decisions").forEach(decision -> decisions.add(decision.asText()));
            if (!source.path("modelKey").isNull()) manifestedKeys.add(source.path("modelKey").asText());
        }
        assertThat(manifestedKeys).containsExactlyInAnyOrderElementsOf(modelKeys);
        assertThat(decisions).containsAll(List.of("CL-011", "CL-012", "CL-013", "CL-014", "CL-015", "CL-016", "CL-017"));
        assertThat(manifest.path("requiredDecisions")).hasSize(7);
    }
}
