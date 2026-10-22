# ADR-0019 — Bases del modelo flexible y conservación de v1

Fecha: 2026-10-21. Estado: adoptada para las bases internas de D2; D2 no está aceptada.

## Contexto

La publicación, el catálogo usado por procesos y la comparación recorrían exactamente
tres niveles. D2 necesita indicadores directos y anidados, mientras ADR-0018 conserva
el transporte v1 y reserva la estructura nueva para v2.

## Decisión

- Ampliar `ModelNode` con `nodeType` opcional y conservar el constructor anterior.
  Un snapshot anterior sin ese campo conserva sus niveles criterio/estándar/indicador,
  UUID y contenido; leerlo no lo reescribe ni inventa clasificación normativa.
- Centralizar el recorrido en `ModelHierarchy`: tipos explícitos admiten categorías
  propias, profundidad variable e indicadores terminales. Rechazar identidades
  repetidas, ciclos por identidad, códigos repetidos por tipo y posiciones hermanas
  duplicadas. Publicación, catálogo y comparación usan el mismo recorrido.
- Comparar también la ubicación del nodo por tipo/código de su padre. Mover un
  indicador debe aparecer como modificación aunque mantenga texto y ordinal.
- Separar los DTO v1 de los objetos de dominio. V1 conserva sus seis campos de nodo,
  rechaza campos nuevos y responde `MODEL_VERSION_REQUIRES_V2` antes de mutar una
  estructura tipada. No se habilita todavía la API v2 ni su publicación.
- Incorporar `RequiredSource` e `IndicatorDefinition` como valores inmutables:
  identidad, pertenencia, códigos normalizados, modalidad, cardinalidad, período y
  referencias de escala/regla por indicador. Esto no implementa un calculador ni
  acredita una regla, estrategia, referencia normativa o aplicabilidad.
- Mantener V009 reservada hasta integrar el esquema relacional completo de D2.
  La prueba de snapshots SQLite verifica compatibilidad del repositorio existente;
  no sustituye la migración ni las pruebas PostgreSQL de T024.

## Consecuencias y trazabilidad

La primera ejecución completó 005/T021. La continuación incorpora
`ModelConfiguration` como campo opcional del snapshot, sin modificar los DTO v1.
Cada indicador referencia su escala/regla y conserva sus fuentes; el catálogo
transporta esos valores y la comparación considera sus cambios. La creación,
edición, publicación y sucesión configuradas utilizan los servicios existentes,
sin duplicar el servicio de publicación ni exponer todavía rutas v2.

Las reglas cuantitativas incompletas permanecen `DRAFT`, con automatización
deshabilitada. Una regla `VALIDATED` necesita metadatos completos y una estrategia
registrada/validada por clave y versión exactas mediante `ValidatedStrategyPort`.
Su implementación por defecto devuelve un conjunto vacío; no se habilita un
intérprete de fórmulas ni se construyen algoritmos académicos en D2. Los estados
de validación deben ser controlados por servidor al integrar los futuros DTO v2;
el cliente no puede autodeclarar una regla validada.

La sucesora conserva identidades de nodos, fuentes, escalas y reglas dentro de
su snapshot. Al materializar V009 se deben calificar esas identidades por versión
(claves compuestas o revisiones equivalentes), evitando colisiones de claves
globales y referencias cruzadas entre versiones. No regenerar UUID históricos
para eludir esa restricción ni reescribir el snapshot publicado.

Los cinco fixtures son configuraciones de software sintéticas, no transcripciones
normativas. 005/T006/T022/T023/T025/T026 se verifican en este bloque de aplicación;
persistencia relacional, procedencia/aplicabilidad y el recorrido web continúan
en T024/T027–T030/T114/T116. Ningún resultado declara los modelos institucionales
corroborados. FR-042, FR-044–FR-047; US2, escenarios 1–4; RNF-011/RNF-018.

Evidencia: [bases D2](../../tests/acceptance/results/d2-model-foundations.md).
Continuación: [publicación por indicador](../../tests/acceptance/results/d2-model-publication.md).
