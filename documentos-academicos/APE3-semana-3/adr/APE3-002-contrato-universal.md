# ADR académico APE3-002 — Contrato común y compatibilidad

**Estado: propuesta pendiente; sin cambio de API pública.**

## Contexto y alternativas

Los controladores actuales retornan Career, AcademicUnit, CatalogPage, SessionView o listas; los errores son Problem Details. La guía pide `success/message/data/errors/meta`. ADR-0018 preserva DTO v1 y mantiene separado el contrato objetivo de 005. Cambiar únicamente el servidor rompería consumidores que leen, por ejemplo, `csrfToken` en raíz o `items` de la página.

Alternativas: conservar contrato actual y justificar excepción docente (compatibilidad inmediata, desviación de guía); migrar de forma coordinada y versionada al sobre (forma común, coste de cliente/contratos/tests); prototipo aislado con sobre (evita romper producto, requiere identificarlo claramente y no suponer cobertura de negocio completa).

## Propuesta

Entregar `contratos/openapi-observado.yaml` como descripción del subconjunto existente y `contratos/openapi-objetivo.yaml` como forma académica propuesta, con indicador explícito de no implementación. No asignar un servidor productivo ficticio ni afirmar disponibilidad de v2. En el objetivo generado: conservar los códigos HTTP documentados, colocar representación en data, errores estructurados en errors y correlación en meta. Las páginas conservan `data.items/page/size/totalElements`; no trasladan ni duplican paginación en meta. Esta forma coincide con el generador y su extensión `x-pagination-decision`, y sigue pendiente de aceptación del equipo/docente. Respuestas 204 no tienen cuerpo; tampoco sobre JSON.

## Consecuencias y decisión pendiente

Los dos documentos tienen estados distintos y no son intercambiables. Antes de implementar el objetivo: acordar versión/frontera, actualizar cliente, filtros, handlers, OpenAPI, pruebas y documentación; comprobar headers y ausencia de cuerpos. Registrar aprobación real y cambios versionados. La decisión sigue pendiente y no acredita cumplimiento del sobre en Spring.
