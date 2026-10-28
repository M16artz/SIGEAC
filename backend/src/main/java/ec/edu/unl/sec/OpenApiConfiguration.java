package ec.edu.unl.sec;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI secOpenApi() {
        return new OpenAPI().info(new Info()
                .title("SEC-UNL | API de evaluación de carreras")
                .version("v1")
                .description("API REST para gestionar procesos de autoevaluación, evidencias, "
                        + "revisión documental, evaluación académica, informes y mejora. "
                        + "Las operaciones requieren sesión y permisos según el ámbito, salvo el inicio de sesión."));
    }

    @Bean
    OpenApiCustomizer organizeOperations() {
        return openApi -> openApi.getPaths().forEach((path, item) -> item.readOperations().forEach(operation ->
                operation.setTags(List.of(tagFor(path)))));
    }

    private String tagFor(String path) {
        if (path.startsWith("/sessions") || path.startsWith("/me") || path.startsWith("/users")
                || path.startsWith("/conflict-exceptions")) return "01 · Acceso e identidad";
        if (path.startsWith("/careers") || path.startsWith("/academic-units")) return "02 · Carreras y unidades";
        if (path.startsWith("/models") || path.startsWith("/model-versions") || path.startsWith("/model-audit"))
            return "03 · Modelos de evaluación";
        if (path.contains("document-review") || path.startsWith("/document-review-inbox"))
            return "06 · Revisión documental";
        if (path.contains("academic-") || path.contains("quantitative-") || path.startsWith("/academic-evaluation"))
            return "07 · Evaluación académica";
        if (path.contains("documentary-")) return "08 · Avance documental";
        if (path.contains("finding") || path.contains("consolidation") || path.contains("report"))
            return "09 · Informes";
        if (path.contains("improvement")) return "10 · Planes de mejora";
        if (path.contains("closure") || path.endsWith("/close") || path.endsWith("/archive"))
            return "11 · Cierre y archivo";
        if (path.startsWith("/processes") || path.startsWith("/teams") || path.startsWith("/activities"))
            return "04 · Procesos y planificación";
        if (path.startsWith("/evidences") || path.startsWith("/evidence-versions"))
            return "05 · Evidencias";
        if (path.contains("audit-events")) return "12 · Auditoría";
        if (path.startsWith("/exchange-packages")) return "13 · Integraciones";
        return "Otros";
    }
}
