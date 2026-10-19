# Plan de implementación: Sistema de Evaluación de Carreras

**Feature**: `001-career-evaluation-platform` | **Fecha**: 2026-10-18 | **Especificación**: [spec.md](../requirements/spec.md)

## Resumen

Construir una aplicación web cliente-servidor para ejecutar el recorrido I0–I6 de una autoevaluación interna: acceso por función y ámbito, catálogo configurable de modelos publicados e inmutables, procesos y asignaciones, evidencia versionada, revisión documental separada, valoración académica, seguimiento y cierre reproducible. I7 incorpora estrategias de valoración solo para algoritmos distintos y validados; I8 reserva adaptadores para importaciones o integraciones heterogéneas. Ninguna extensión introduce dependencias operativas en el MVP.

La solución será un backend monolítico modular con API REST y un cliente web. La persistencia relacional mantendrá metadatos, estados, decisiones y auditoría; los binarios se gestionarán detrás de un puerto de almacenamiento, con adaptador local para desarrollo y compatibilidad futura con S3. La seguridad se aplicará en backend con sesión revocable, autorización por política y ámbito, protección CSRF y auditoría.

## Contexto técnico

**Lenguajes/versiones**: Java 25 LTS; TypeScript 6.0 mediante Angular 22; SQL PostgreSQL 18

**Dependencias principales**: Spring Boot 4.1.x, Spring Security 7.1.x, Spring Data JPA/Hibernate, Flyway, springdoc/OpenAPI 3.1, Angular Material/CDK, RxJS 7.4+, Bean Validation

**Almacenamiento**: PostgreSQL 18 para datos estructurados; sistema de archivos local fuera de PostgreSQL para el MVP mediante un puerto reemplazable por almacenamiento compatible con S3

**Pruebas**: JUnit 5, AssertJ, ArchUnit, Testcontainers PostgreSQL, REST Assured/Spring MockMvc, Vitest, Angular Testing Library y Playwright; pruebas de rendimiento reproducibles con k6

**Plataforma objetivo**: servidor Linux o Windows con Java y PostgreSQL; navegadores incluidos por Angular Baseline, acotados por RNF-015; desarrollo y pruebas disponibles sin Docker

**Tipo de proyecto**: aplicación web cliente-servidor con backend monolítico modular y documentación Quarto

**Objetivos de rendimiento**: p95 ≤ 2 s en operaciones interactivas; p95 ≤ 15 s para archivos de hasta 25 MiB; 30 sesiones concurrentes y escala piloto de RNF-008

**Restricciones**: una carrera, una versión de modelo y un proceso en ejecución para el piloto; configuraciones sintéticas representativas de cinco modelos; archivos fuera de PostgreSQL; I7–I8 no bloquean I0–I6; GitHub Actions solo manual; sin despliegue productivo; sin dependencia obligatoria de Docker

**Escala/alcance**: 100 usuarios, 500 indicadores, 10 000 versiones de evidencia y 100 GiB de archivos en el entorno piloto

## Comprobación constitucional

### Puerta previa a investigación

| Principio | Resultado | Evidencia del plan |
|---|---|---|
| I. Separación estricta del dominio | CUMPLE | Módulos y entidades distintos para evidencia, decisión documental y valoración académica; no hay transición implícita de aceptación a cumplimiento. |
| II. SDD y trazabilidad | CUMPLE | IDs FR/RNF/SC, OpenAPI, matriz de trazabilidad y tareas/pruebas vinculadas. |
| III. Incrementos verticales | CUMPLE | I0–I6 son recorridos demostrables; I7–I8 solo exponen contratos opcionales. |
| IV. Seguridad por función y ámbito | CUMPLE | Autorización en aplicación/dominio por función, carrera, proceso e indicador; frontend no decide permisos. |
| V. Inmutabilidad, versionado y auditoría | CUMPLE | Modelos publicados, evidencias remitidas y decisiones finales usan revisión append-only y auditoría. |
| VI. Arquitectura modular | CUMPLE | Monolito modular con capas por capacidad y pruebas ArchUnit de dependencias. |
| VII. Calidad y documentación viva | CUMPLE | Pruebas contractuales, integración, dominio, rendimiento, accesibilidad, documentación Quarto y quickstart. |

No se solicitan excepciones. Ninguna decisión CL-001–CL-010 se codifica como regla institucional fija; las políticas del piloto son configurables y sintéticas.

### Puerta posterior al diseño

**Resultado**: CUMPLE. El modelo de datos mantiene conceptos separados, OpenAPI aplica seguridad uniforme, el contrato de errores evita filtración, la estrategia de archivos preserva atomicidad y el diseño reserva extensiones I7–I8 sin dependencias inversas hacia el MVP. Los ADR documentan el monolito modular, el puerto de archivos, el catálogo configurable de modelos y la separación entre estrategias de valoración y adaptadores de importación.

## Arquitectura

### Contexto

El cliente Angular consume exclusivamente la API REST. El backend aplica autenticación, autorización, reglas de estado y auditoría, y accede a PostgreSQL y al almacenamiento de archivos mediante adaptadores. Quarto publica documentación reproducible. Servicios institucionales y canales de notificación quedan fuera del límite operativo del MVP.

### Módulos del backend

| Módulo | Responsabilidad | Puede depender de |
|---|---|---|
| `identityaccess` | usuarios, credenciales, sesiones, funciones, ámbitos y conflictos | `shared`, `audit` |
| `evaluationmodel` | catálogo de modelos generales/específicos, versiones publicadas, jerarquías flexibles, indicadores, fuentes y escalas | `shared`, `audit` |
| `selfassessment` | carrera, proceso, estados, asignaciones y cierre | `identityaccess` (puertos), `evaluationmodel` (puertos), `audit` |
| `evidence` | borradores, versiones remitidas, metadatos e integridad de archivos | `selfassessment` (puertos), `filestorage`, `audit` |
| `documentreview` | decisiones documentales append-only | `evidence` (puertos), `identityaccess` (políticas), `audit` |
| `academicevaluation` | valoraciones y revisiones académicas; registro/fábrica de estrategias opcional en I7 | `documentreview` y `evaluationmodel` (puertos), `identityaccess`, `audit` |
| `reporting` | avance, trazabilidad, resumen de cierre y exportaciones | vistas/puertos de los módulos, nunca repositorios internos |
| `integration` | adaptadores de importación e integraciones externas I8 y publicación de eventos complementarios | puertos públicos; ningún módulo MVP depende de él |
| `audit` | eventos inmutables y consulta autorizada | `shared` |
| `shared` | identificadores, reloj, errores, correlación y tipos transversales mínimos | ninguno |

Cada módulo se divide en `api`, `application`, `domain` e `infrastructure`. Los controladores llaman casos de uso; las reglas residen en dominio/aplicación; repositorios y almacenamiento implementan puertos. ArchUnit impedirá accesos a paquetes `infrastructure` o `domain` internos de otros módulos.

### Estrategia de seguridad

- Sesiones revocables almacenadas del lado del servidor, cookie `HttpOnly`, `Secure` en entornos TLS y `SameSite=Lax`; tokens CSRF para operaciones mutables.
- Contraseñas con Argon2id y parámetros configurables; limitación de intentos según RNF-003.
- Autorización en dos pasos: capacidad por función y filtro/validación de ámbito sobre el objeto solicitado.
- Separación de cuentas técnicas; no reciben casos de uso de revisión o valoración.
- Descargas mediante endpoint autorizado, sin rutas directas al almacenamiento.
- Respuesta `ProblemDetail` estable con código de dominio y correlación; mensajes 401/403 no confirman existencia de objetos fuera de ámbito.

### Consistencia e inmutabilidad

- Bloqueo optimista con campo `version` en agregados editables.
- Publicación/remisión/finalización crea registros inmutables; rectificaciones añaden revisiones y no actualizan el registro histórico.
- Cada proceso conserva la referencia inmutable a la versión de modelo seleccionada al iniciarse; publicar otra versión nunca migra procesos existentes de manera implícita.
- La carga usa archivo temporal, cálculo SHA-256, validación y promoción; una transacción fallida deja el temporal elegible para limpieza, nunca una evidencia remitida falsa.
- El cierre toma un bloqueo del proceso, revalida prerrequisitos y guarda un resumen inmutable dentro de la misma transacción de dominio.
- Los eventos de auditoría se insertan con cada cambio crítico; no existe operación ordinaria de actualización o eliminación.

## Estructura del proyecto

### Documentación de la funcionalidad

```text
docs/
├── requirements/spec.md
├── planning/
│   ├── plan.md
│   └── tasks.md
├── architecture/
│   ├── research.md
│   └── data-model.md
├── adr/
│   ├── 0001-modular-monolith.md
│   ├── 0002-file-storage-port.md
│   ├── 0015-configurable-evaluation-models.md
│   └── 0016-evaluation-strategies-import-adapters.md
├── technical/
│   ├── quickstart.md
│   └── contracts/
│       ├── openapi.yaml
│       └── events-asyncapi.yaml
└── testing/
    ├── traceability.md
    └── checklists/
        ├── governance.md
        └── requirements.md
```

### Código fuente

```text
backend/
├── pom.xml
└── src/
    ├── main/java/ec/edu/unl/sec/
    │   ├── shared/
    │   ├── audit/
    │   ├── identityaccess/
    │   ├── evaluationmodel/
    │   ├── selfassessment/
    │   ├── evidence/
    │   ├── documentreview/
    │   ├── academicevaluation/
    │   ├── reporting/
    │   ├── integration/
    │   └── filestorage/
    ├── main/resources/db/migration/
    └── test/java/ec/edu/unl/sec/

frontend/
├── package.json
└── src/app/
    ├── core/
    ├── shared/
    └── features/
        ├── access/
        ├── models/
        ├── processes/
        ├── evidence/
        ├── reviews/
        ├── evaluations/
        └── reporting/

tests/
├── e2e/
├── performance/
├── fixtures/
└── acceptance/

docs/
├── technical/
└── user/
```

**Decisión de estructura**: se conservan `backend/`, `frontend/`, `tests/` y `docs/` ya preparados. La aplicación móvil existente permanece fuera del MVP y no será dependencia del cliente web ni de la API.

## Fases de entrega

1. **Fase 0 — Investigación**: cerrar decisiones técnicas en [research.md](../architecture/research.md), sin resolver normativas CL como si fueran reglas oficiales.
2. **Fase 1 — Diseño**: aprobar [data-model.md](../architecture/data-model.md), contratos, ADR, quickstart y matriz de trazabilidad.
3. **I0 — Acceso**: identidad, sesiones, funciones, ámbitos, conflictos y auditoría base.
4. **I1 — Modelos**: catálogo común, jerarquía flexible, edición, validación, publicación y consulta de versiones; sus pruebas de aceptación quedan disponibles para la apertura de procesos de I2.
5. **I2 — Procesos**: configuraciones representativas de modelos, carrera piloto, asociación inmutable proceso–versión, asignaciones, estados y reapertura.
6. **I3 — Evidencias**: borradores, archivos, remisión, sucesión e integridad.
7. **I4 — Revisión documental**: decisiones separadas y rectificación append-only.
8. **I5 — Valoración académica**: escala, sustentos aceptados y revisiones.
9. **I6 — Seguimiento y cierre**: cálculo, tablero, resumen, informe y exportación.
10. **I7 — Estrategias de valoración**: interfaz común, registro/fábrica y estrategias verificadas; una fórmula no registrada o no validada nunca se ejecuta.
11. **I8 — Importaciones e integraciones**: adaptadores únicamente para traducir formatos o servicios externos a contratos internos; no se usan para elegir entre modelos del catálogo.

Cada incremento actualiza migración, OpenAPI, pruebas, trazabilidad, guía Quarto y quickstart. Las migraciones se validan desde base limpia y con un recorrido documentado de respaldo/restauración cuando corresponda.

## Estrategia de pruebas

- **Dominio**: transiciones, separación de funciones, versionado, inmutabilidad y fórmula.
- **Configurabilidad de modelos**: fixtures del modelo general 2024 y estructuras sintéticas representativas de Medicina, Enfermería, Odontología y Derecho; se valida estructura, no la totalidad de sus cálculos académicos.
- **Extensiones**: pruebas del registro de estrategias en I7 y pruebas contractuales por adaptador de importación en I8, siempre con I0–I6 ejecutable sin ambos.
- **Arquitectura**: dependencias entre módulos/capas con ArchUnit.
- **Persistencia/migración**: PostgreSQL real mediante Testcontainers cuando esté disponible y PostgreSQL local como camino sin Docker; nunca H2 para aceptación de SQL.
- **Contrato**: OpenAPI lint y pruebas de endpoint/errores/autorización.
- **Integración**: recorridos de cada incremento con archivos temporales y datos sintéticos.
- **Cliente**: componentes, accesibilidad automatizada y recorridos Playwright.
- **Rendimiento**: k6 contra dataset RNF-008 con p95 de RNF-006/RNF-007.
- **Seguridad**: matriz función×ámbito×operación, CSRF, enumeración, sesiones, cargas y datos sensibles en logs.
- **Usabilidad**: protocolo moderado con al menos 20 participantes sintéticos de los cinco perfiles, una tarea representativa por función y medición de primer intento conforme SC-007.

## Puertas de calidad

- Compilación, formato, análisis estático y pruebas por módulo.
- OpenAPI válido y sin ruptura no documentada.
- Migraciones aplicables desde cero y sobre la versión anterior aceptada.
- 100 % de reglas críticas cubiertas por pruebas vinculadas a FR/RNF.
- Sin secretos ni datos institucionales reales en repositorio o artefactos.
- Documentación técnica/usuario y trazabilidad actualizadas.
- Workflows de GitHub conservan exclusivamente `workflow_dispatch`; no hay despliegue automático.
- Ningún hallazgo CRITICAL de `$speckit-analyze` antes de `$speckit-implement`.

## Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| CL-001–CL-010 cambian tras validación | Políticas versionadas/configurables y procedencia explícita; no codificar equivalencias oficiales. |
| Confusión entre aceptación y cumplimiento | Agregados, endpoints, permisos, textos y pruebas independientes. |
| Inconsistencia entre archivo y metadatos | Flujo temporal/promoción, integridad SHA-256 y reconciliación segura. |
| Autorización filtrada solo en UI | Pruebas negativas de backend y políticas sobre cada caso de uso. |
| Crecimiento de dependencias internas | Puertos públicos, ArchUnit y ADR obligatorio para excepciones. |
| I7–I8 contaminan el MVP | Módulo de integración periférico y pruebas del MVP sin sus componentes. |
| Un modelo por carrera deriva en cinco implementaciones | Catálogo versionado, jerarquía de nodos y servicios compartidos; las diferencias se expresan como configuración. |
| Uso indiscriminado de patrones | Strategy solo para algoritmos de valoración validados; Adapter solo en límites de importación o integración externa. |

## Seguimiento de complejidad

No existen violaciones constitucionales que requieran justificación. El patrón de puertos se limita a límites externos y dependencias entre módulos; no se introduce una capa genérica adicional para cada entidad. El catálogo resuelve la selección de modelos del MVP sin Adapter. Strategy aparece en I7 solo si hay algoritmos distintos y verificados; Adapter aparece en I8 solo ante formatos o servicios externos heterogéneos.

## Decisiones de diseño para modelos y valoración

- El catálogo de modelos es versionado y configurable; cada modelo declara organismo emisor, denominación, versión, fecha y documento fuente.
- La estructura se representa como una jerarquía flexible de nodos tipados, sin tablas o servicios independientes por carrera.
- Las definiciones de indicadores pertenecen a una versión del modelo; las ejecuciones, evidencias y valoraciones pertenecen a un proceso que referencia esa versión.
- La asociación proceso–versión queda inmutable desde el inicio del proceso y se conserva históricamente.
- I7 usa `EvaluationStrategy` con Factory/Registry para seleccionar únicamente implementaciones registradas y validadas.
- I8 usa Adapter en cada límite de importación o integración heterogénea; el dominio recibe un contrato interno común.
- ADR-015 y ADR-016 contienen la justificación, beneficios, costes y alternativas de estas decisiones.
