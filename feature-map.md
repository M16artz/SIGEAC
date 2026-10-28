# Mapa de funcionalidades — SIGEAC

## 1. Propósito

SIGEAC es una aplicación web para gestionar procesos de autoevaluación de carreras: configurar modelos, organizar equipos y actividades, registrar y revisar evidencias, evaluar indicadores, elaborar informes, dar seguimiento a planes de mejora y cerrar/archivar procesos.

> El sistema registra y organiza decisiones institucionales. Sus resultados no constituyen una acreditación. Las competencias y reglas institucionales deben validarse con la normativa correspondiente.

## 2. Aplicación web

Frontend Angular ubicado en `frontend/`. Las rutas descritas abajo corresponden a las vistas disponibles en la aplicación.

### Acceso y administración

| Vista | Ruta | Funcionalidad |
|---|---|---|
| Inicio y ámbitos | `/access` | Muestra identidad, funciones y ámbitos autorizados. |
| Inicio de sesión | `/access/login` | Inicia una sesión para una identidad habilitada. |
| Usuarios y asignaciones | `/access/users` | Administra usuarios, funciones y asignaciones. |
| Conflictos y excepciones | `/access/conflicts` | Registra excepciones justificadas a reglas de conflicto. |
| Auditoría de acceso | `/access/audit` | Consulta eventos de acceso y administración. |

### Catálogos y modelos

| Vista | Ruta | Funcionalidad |
|---|---|---|
| Catálogo de carreras | `/careers` | Consulta y administra carreras y unidades académicas. |
| Editor de modelos | `/models` | Configura modelos, versiones, estructura, indicadores, fuentes y reglas de evaluación. |
| Historial de modelos | `/models/history` | Consulta versiones y sus estados. |
| Comparación de modelos | `/models/compare` | Compara dos versiones de un modelo. |
| Auditoría de modelos | `/models/audit` | Consulta cambios y decisiones sobre modelos. |

El catálogo contempla modelos General, Medicina, Enfermería, Odontología y Derecho como configuraciones del modelo común.

### Procesos y planificación

| Vista | Ruta | Funcionalidad |
|---|---|---|
| Procesos: listado y creación | `/processes` | Consulta procesos y prepara uno nuevo para una carrera, periodo y modelo. |
| Detalle del proceso | `/processes/:processId` | Muestra estado, datos, autorizaciones e información del proceso. |
| Planificación | `/processes/:processId/planning` | Organiza equipos, membresías y asignaciones. |
| Actividades | `/processes/:processId/activities` | Gestiona tareas, responsables, fechas y avance. |

### Evidencias y revisión documental

| Vista | Ruta | Funcionalidad |
|---|---|---|
| Listado de evidencias | `/evidence` | Consulta evidencias y versiones. |
| Crear evidencia | `/evidence/new` | Registra metadatos, fuentes cubiertas y contenido de respaldo. |
| Detalle de versión | `/evidence/versions/:versionId` | Consulta una versión de evidencia y las acciones permitidas. |
| Editar evidencia | `/evidence/versions/:versionId/edit` | Corrige un borrador. Las versiones remitidas se conservan inmutables. |
| Historial de evidencia | `/evidence/versions/:versionId/history` | Consulta la sucesión de versiones y decisiones. |
| Auditoría de evidencias | `/evidence/audit` | Consulta eventos relacionados con evidencias. |
| Bandeja de revisión | `/reviews` | Revisa evidencias remitidas y registra decisiones justificadas por fuente/cobertura. |

El respaldo admite archivos, referencias externas y fuentes estructuradas, sujeto a validación. Una corrección genera una versión sucesora.

### Evaluación y seguimiento

| Vista | Ruta | Funcionalidad |
|---|---|---|
| Evaluación manual | `/evaluations` | Registra valoraciones académicas de indicadores y su justificación. |
| Evaluación cuantitativa | `/evaluations/quantitative` | Registra/verifica variables y ejecuta cálculos admitidos por el modelo. |
| Conciliación académica | `/evaluations/reconciliation` | Registra y resuelve discrepancias entre valoraciones. |
| Panel documental | `/reporting` | Consulta avance y cobertura de fuentes documentales. |
| Historial documental | `/reporting/history` | Consulta cortes históricos del avance documental. |
| Cierre documental | `/reporting/closure` | Revisa condiciones y registra el cierre documental. |

La cobertura documental, la revisión de evidencias y la evaluación académica se mantienen como estados distintos.

### Informes, mejora y archivo

| Vista | Ruta | Funcionalidad |
|---|---|---|
| Hallazgos e informes | `/reports` | Consolida resultados, registra hallazgos y gestiona versiones, observaciones y aprobación de informes. |
| Plan de mejora | `/improvement` | Administra acciones, responsables, metas, fechas, avances y verificaciones. |
| Cierre integral y archivo | `/archive` | Revisa bloqueos, registra el cierre y consulta el historial de archivo. |

## 3. Backend y API

Backend Java/Spring Boot ubicado en `backend/`. La API REST tiene el prefijo `/api/v1`; el contrato principal está en [`docs/technical/contracts/openapi.yaml`](docs/technical/contracts/openapi.yaml).

| Módulo | Capacidades principales |
|---|---|
| `identityaccess` | Sesiones, identidad actual, usuarios, asignaciones, excepciones y auditoría de acceso. |
| `selfassessment` | Carreras, unidades, procesos, autorizaciones, transiciones, equipos, membresías, asignaciones y actividades. |
| `evaluationmodel` | Modelos y versiones, configuración/publicación, sucesión, comparación y auditoría. |
| `evidence` | Evidencias y versiones, metadatos, archivos, referencias externas, fuentes estructuradas, coberturas, envío e historial. |
| `documentreview` | Bandeja, decisiones de revisión documental y solicitudes de corrección. |
| `academicevaluation` | Valoración manual, evaluación cuantitativa, verificación, cálculo y conciliación de desacuerdos. |
| `reporting` | Progreso documental, instantáneas históricas y cierre documental. |
| `governance` | Hallazgos, consolidaciones, versiones de informe, socialización, observaciones, respuestas y aprobación. |
| `improvement` | Planes de mejora, acciones, avances y decisiones de seguimiento/verificación. |
| `archive` | Previsualización de cierre, cierres integrales y archivo. |
| `audit` | Registro y consulta de eventos auditables. |
| `filestorage` | Almacenamiento local, validación e integridad de archivos, recuperación y reconciliación. |
| `integration` | Límites/adaptadores para integraciones e intercambio de paquetes. |
| `shared` | Manejo común de errores API, identificadores, control de concurrencia y procedencia. |

### Documentación interactiva de API

Swagger UI está disponible en desarrollo en `/api/v1/swagger-ui/index.html` y el documento generado en `/api/v1/v3/api-docs`, con backend local en el puerto 8080. La interfaz agrupa operaciones por módulo y permite probarlas. Los endpoints de negocio mantienen autenticación y autorización; en perfil de producción Swagger/OpenAPI está desactivado.

## 4. Flujos funcionales P01–P16

El catálogo detallado, con actores, precondiciones, pasos, alternativas y resultados, está en [`docs/requirements/process-flows.md`](docs/requirements/process-flows.md). Resumen:

| Flujo | Alcance |
|---|---|
| P01 | Autenticación, sesiones, usuarios y permisos. |
| P02 | Administración de carreras, unidades y ámbitos. |
| P03 | Registro, versionado y publicación de modelos. |
| P04 | Estructura, indicadores, fuentes, metodologías y reglas del modelo. |
| P05 | Creación, autorización y activación del proceso. |
| P06 | Equipos, asignaciones y planificación de actividades. |
| P07 | Registro de evidencias y coberturas. |
| P08 | Envío de versiones a revisión. |
| P09 | Revisión documental y decisión por fuente. |
| P10 | Subsanación mediante versiones sucesoras. |
| P11 | Seguimiento, cortes y cierre documental. |
| P12 | Valoración académica manual y cuantitativa; conciliación. |
| P13 | Consolidación y registro de hallazgos. |
| P14 | Versionado, socialización, observaciones y aprobación del informe. |
| P15 | Planes, acciones, avances y verificación de mejora. |
| P16 | Cierre integral y archivo del proceso. |

## 5. Capacidades transversales

- Autenticación y autorización por función y ámbito vigente (carrera, proceso e indicador).
- Estados explícitos y transiciones controladas para procesos y entregables.
- Historial auditable de operaciones y decisiones.
- Versiones publicadas/finalizadas inmutables; las correcciones se registran como sucesoras.
- Validación de concurrencia para cambios que requieren versión vigente.
- Cobertura documental y valoración académica independientes.
- Manejo de errores API con respuestas estructuradas.
- Almacenamiento de archivos separado de la base de datos, con límites e inspección.
- Interfaz Angular con rutas protegidas según acceso.
- No presentar porcentajes documentales o valoraciones como acreditación.

## 6. Documentación del proyecto

- [Manual de usuario](docs/user/index.qmd) y guías por módulo en `docs/user/`.
- [Flujos P01–P16](docs/requirements/process-flows.md).
- [Reglas de dominio y responsabilidades](docs/requirements/domain-rules.md).
- [Contrato OpenAPI](docs/technical/contracts/openapi.yaml).
- [Arquitectura de flujos completos](docs/architecture/complete-workflows.md).
- [Trazabilidad](docs/testing/traceability.md) y [escenarios de aceptación](docs/testing/process-acceptance.md).
- [Guía técnica rápida](docs/technical/quickstart.md).

