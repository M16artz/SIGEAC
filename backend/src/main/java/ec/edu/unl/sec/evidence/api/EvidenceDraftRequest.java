package ec.edu.unl.sec.evidence.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class EvidenceDraftRequest {
    private EvidenceDraftRequest() {}

    public record Create(
            @NotNull UUID processId,
            @NotBlank @Size(max = 300) String title,
            @NotBlank @Size(max = 8000) String description,
            @NotBlank @Size(max = 4000) String provenance,
            @NotNull LocalDate coveredFrom,
            @NotNull LocalDate coveredTo,
            @NotEmpty List<UUID> indicatorIds) {}

    public record Update(
            @NotBlank @Size(max = 300) String title,
            @NotBlank @Size(max = 8000) String description,
            @NotBlank @Size(max = 4000) String provenance,
            @NotNull LocalDate coveredFrom,
            @NotNull LocalDate coveredTo,
            @NotEmpty List<UUID> indicatorIds) {}
}
