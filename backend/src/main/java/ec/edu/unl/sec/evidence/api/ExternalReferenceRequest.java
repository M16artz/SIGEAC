package ec.edu.unl.sec.evidence.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExternalReferenceRequest(@NotBlank @Size(max = 2048) String uri,
        @NotBlank @Size(max = 1000) String description) {}
