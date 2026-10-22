# Bases internas de D2 — avance, no aceptación de D2

Fecha: 2026-10-21. Base: `b263a56` (`parche de documentacion y correcciones`).
Datos exclusivamente sintéticos. D1 se conserva; D2–D8 siguen sin aceptación.

## Análisis previo de especificación, plan y tareas

Se contrastaron US2 (escenarios 1–3), el modelo de datos de 005, T021–T030/T116,
las reglas de dominio y ADR-0018. No hay contradicción crítica para este bloque
interno. Hay dos restricciones para continuar: v1 conserva sus DTO y V009 debe
integrar el esquema completo antes de aplicarse. Publicar no acredita vigencia.
Las reglas por indicador y la aplicabilidad pendiente impiden declarar D2 cerrada.

## Alcance comprobable

- `FlexibleModelHierarchyTest`: indicador directo, anidado y categoría configurable;
  compatibilidad legada; ciclos por UUID; duplicados de código/UUID/posición;
  indicadores terminales y ramas vacías.
- `ModelComparisonServiceTest`: comparación recursiva y movimiento entre padres,
  conservando el formato de diferencias v1.
- `FlexibleModelPersistenceTest`: escritura/lectura de tipos en snapshots SQLite,
  lectura sin reescritura de JSON anterior sin `nodeType`, conservación de UUID
  y catálogo con indicadores directos/anidados.
- `RequiredSourceTest`: cuatro modalidades, fuentes opcionales, máximo abierto,
  cardinalidades inválidas, pertenencia, códigos normalizados únicos y colecciones
  inmutables; período y referencias de escala/regla propios del indicador.
- `EvaluationModelControllerTest` y `LegacyModelCompatibilityTest`: transporte v1,
  rechazo de campos nuevos y rechazo de modificación/publicación/sucesión tipadas
  antes de guardar cambios. Los tipos nuevos requieren la futura API v2.

## Reproducción

Usar JDK 25 y ejecutar desde `backend/`:

```powershell
.\mvnw.cmd test
```

Para este entorno se descargaron Maven 3.9.16 y Eclipse Temurin 25.0.4.1+1 en
`backend/target`, con SHA-256 del paquete JDK comprobado frente al manifiesto de
Adoptium. La caché Maven se mantuvo también en `target`; no cambia el toolchain
declarado del proyecto. El perfil SQLite permite las pruebas sin PostgreSQL.

Resultado final de `mvnw.cmd test` con Java 25: **136 casos, 128 aprobados,
0 fallos, 0 errores y 8 omitidos**. Los ocho omitidos corresponden a pruebas
PostgreSQL condicionadas a `SEC_RUN_DATABASE_TESTS`; no se habilitó esa variable
ni se utilizó una base persistente. ArchUnit y el arranque SQLite aprobaron.
Los seis casos HTTP de modelos y los dos casos de protección de comandos v1
forman parte de esa ejecución. Informes locales: `backend/target/surefire-reports`.

También aprobaron los tres contratos OpenAPI (común, 004 y 005), validados con
Redocly 2.65.0. `check-contract-compatibility.mjs` informó 70 operaciones comunes,
77 objetivo, 12 compartidas y **0 incompatibilidades**. Se usó `yaml` 2.9.1,
la versión del lockfile, con herramientas temporales en `backend/target/contract-tools`.
`git diff --check` aprobó. No se ejecutaron Angular, navegador, rendimiento ni
aceptación institucional para este bloque interno.

Una primera ejecución focalizada con Java 26 aprobó 30 casos, pero produjo
avisos de compatibilidad de ArchUnit. La evidencia definitiva es la regresión
con Java 25, sin omitir el enforcer ni cambiar `pom.xml`.

## Trabajo siguiente

005/T021 quedó cerrado en esta primera ejecución. La [continuación de publicación
por indicador](d2-model-publication.md) registra el cierre posterior de
T006/T022/T023/T025/T026 y su regresión. Continúan T114/T116/T027 para procedencia
y aplicabilidad, V009/T024, contrato v2/T028 y editor/T029 antes de T030. Los
resultados de 136 casos anteriores corresponden únicamente a esta primera base.
No cambiar migraciones anteriores ni convertir fórmulas de avance en fórmulas
académicas. No hay aceptación institucional.
