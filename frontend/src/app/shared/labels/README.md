# Presentación de datos de SIGEAC

Los identificadores siguen en los modelos, valores de opciones, enlaces y cuerpos
HTTP. No deben utilizarse como alternativa visual a una denominación que falta.

## Fuentes reales y límites

- `EntityNamesService` reutiliza `CareerApi`, `ProcessApi`, `AccessApi` y la identidad
  autenticada. Recorre las páginas del catálogo y del registro de usuarios.
- Las páginas proveen su instancia del catálogo para reutilizarla entre sus
  componentes, cancelar lecturas al salir y consultar nombres actuales al regresar.
- El directorio completo de personas se consulta solo para administración funcional;
  los demás perfiles no adquieren permisos nuevos. La identidad propia sigue
  disponible sin esa consulta. Las respuestas denegadas no se sortean.
- Las referencias de evidencias y fuentes usan `EvidenceApi.getVersion` y
  `requiredSources`, con lecturas compartidas por referencia y reinicio al cambiar
  la identidad. Los errores no revelan el ID ni el diagnóstico del servidor.
- «Carrera no disponible», «Fuente no disponible» y equivalentes son estados de
  consulta, no denominaciones inventadas. No se intenta obtener recursos fuera
  del ámbito permitido para sustituir esos estados.
- No existe una consulta nominal de políticas de incompatibilidad en las API
  actuales. El selector utiliza `environment.conflictPolicies`: metadatos de
  identificación y denominación de políticas **ya existentes** en el despliegue.
  Producción no presupone ninguna; si faltan esos metadatos, muestra «Sin política
  disponible», sin permitir introducir UUID ni inventar una política. La configuración
  de demostración describe únicamente la pareja responsable/revisor de
  `SyntheticAccessData`. El backend sigue comprobando la política y los permisos.
  Tampoco se inventan nombres de designaciones ajenas cuando no existe una consulta
  autorizada para resolverlas.
- Los selectores de personas conservan una entrada por referencia, desplegable,
  cuando el directorio ajeno no está disponible. Así se conserva el flujo existente
  de usuarios autorizados sin exponer IDs en las opciones habituales.
- La edición estructural de modelos y las auditorías siguen siendo vistas
  especializadas: conservan los datos técnicos necesarios para su función.

`accessDate` reconoce el instante exacto `9999-12-31T00:00:00Z`, usado por
`IdentityAdministrationService` al omitir la fecha final. No convierte cualquier
fecha lejana en una vigencia indefinida. Las fechas civiles no se reinterpretan
como instantes UTC para mostrarlas.

`userErrorMessage` usa textos locales y códigos conocidos para describir errores,
sin enviar diagnósticos crudos a la interfaz ni cambiar el tratamiento HTTP.
Las confirmaciones y fallos de operaciones conservan `NotificationService`.

## Entorno de demostración

Producción y desarrollo normal usan `src/environments/environment.ts`, con
`demonstration: false`. Solo la configuración explícita `demonstration` reemplaza
ese archivo. Ejecución: `npm start -- --configuration demonstration`.

El aviso se muestra una sola vez, en el contenido del layout autenticado, nunca
en el login. Las plantillas sintéticas de creación de modelos y procesos quedan
limitadas a esa configuración. No se añadieron variables ni reglas al backend.

## Archivos de esta mejora

Rutas relativas a `frontend/`; las extensiones agrupadas identifican archivos distintos.

| Área | Archivos |
| --- | --- |
| Configuración | `angular.json`; `src/environments/environment.ts`, `environment.demonstration.ts` |
| Layout | `src/app/core/layout/app-layout.component.ts`, `.html` |
| Presentación compartida | `src/app/shared/labels/entity-names.service.ts`, `.spec.ts`; `access-date.ts`, `.spec.ts`; `evidence-reference.component.ts`, `.spec.ts`; `person-picker.component.ts`, `.spec.ts`; `user-error-message.ts`, `.spec.ts`; este `README.md` |
| Acceso | `src/app/features/access/data-access/access.errors.ts`; `pages/access-home.page.ts`, `.html`, `.scss`, `.spec.ts`; `pages/identity-admin.page.ts`, `.html`, `.spec.ts`; `pages/conflict-exceptions.page.ts`, `.html`, `.spec.ts`; `ui/assignment-form.component.ts` |
| Carreras | `src/app/features/careers/pages/career-catalog.page.ts` |
| Procesos | `src/app/features/processes/pages/process-create.page.ts`, `process-detail.page.ts`, `process-activities.page.ts`, `process-planning.page.ts`, `process-planning.page.spec.ts`; `ui/process-state-controls.component.ts`, `.spec.ts`; `ui/process-assignment-form.component.ts` |
| Modelos | `src/app/features/models/data-access/model.models.ts` (campos nominales ya existentes en la respuesta de comparación); `pages/model-editor.page.ts`, `model-compare.page.ts`; `ui/publication-panel.component.ts` |
| Evidencias | `src/app/features/evidence/ui/structured-evidence.component.ts` |
| Revisión | `src/app/features/reviews/pages/document-review-inbox.page.ts`, `.spec.ts` |
| Evaluación | `src/app/features/evaluations/pages/indicator-evaluation.page.ts`, `.spec.ts`; `quantitative-evaluation.page.ts`, `.spec.ts`; `academic-disagreement.page.ts`, `.spec.ts` |
| Seguimiento | `src/app/features/reporting/pages/documentary-dashboard.page.ts`, `.spec.ts`; `documentary-history.page.ts`, `.spec.ts` |
| Informes, mejora y archivo | `src/app/features/reports/pages/report-workflow.page.ts`; `src/app/features/improvement/pages/improvement-plan.page.ts`, `.spec.ts`; `src/app/features/archive/pages/process-archive.page.ts` |

No se modificaron los endpoints, los identificadores de los modelos internos,
las rutas, las reglas de autorización, el backend ni la base de datos.
