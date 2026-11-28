# Notificaciones de SIGEAC

`NotificationService` es un singleton. El layout autenticado aloja un único
`ToastContainerComponent`, por lo que los avisos sobreviven a la navegación.
Al salir al login se eliminan todos los mensajes y temporizadores.

## Uso

```ts
private readonly notifications = inject(NotificationService);

// Dentro de next, una vez confirmada la operación real:
this.notifications.success('Usuario registrado correctamente', 'La cuenta se creó con éxito.');

// Dentro de error, sin mostrar error.message ni detalles del backend:
this.notifications.error('No se pudo guardar', 'Revise la información e inténtelo nuevamente.');

// Advertencia temporal de una operación, no cada error de campo:
this.notifications.warning('Falta declarar la cobertura', 'Declare una fuente antes de remitir.');

this.notifications.info('Información actualizada', 'Los datos ya están disponibles.');
```

Los métodos admiten un tercer argumento `{key: 'operacion:recurso'}` para identificar
avisos equivalentes. Por defecto se deduplican por tipo, título y descripción,
tanto entre las tarjetas visibles como en la cola. No notifique en el padre y
en el hijo por la misma petición ni añada un interceptor HTTP genérico.

## Comportamiento

Las variantes visuales se definen solo en `toast-container.component.scss`:
éxito verde pastel, error rojo pastel, advertencia amarillo pastel e información
azul pastel. Cada tipo conserva su icono, título de contraste alto, borde suave,
descripción y cierre por teclado. No dependen únicamente del color.

- Hasta tres tarjetas visibles; el resto espera su turno.
- Los éxitos se cierran tras cuatro segundos visibles, más la transición de salida.
- Error, advertencia e información permanecen hasta el cierre manual.
- El tiempo se pausa al pasar el puntero, enfocar una tarjeta, ocultar la pestaña,
  abrir el perfil, abrir la navegación móvil o mostrar un diálogo.
- El contenedor se sitúa bajo el encabezado/perfil y no se muestra mientras su
  menú está abierto. Usa un popover manual, con posicionamiento fijo como fallback.
- No mueve el foco al aparecer. Al cerrar mediante teclado, conserva una ruta
  de foco hacia otra tarjeta o devuelve el foco al control anterior.
- Usa ariaNotify cuando existe y regiones polite/assertive estables como fallback,
  sin anunciar el botón de cerrar junto al mensaje.
- No interpreta HTML. Las cadenas que parecen diagnósticos, códigos internos,
  UUID o URLs se sustituyen por mensajes seguros. La defensa principal sigue
  siendo redactar mensajes locales en español, no enviar texto crudo del backend.

## Qué no migrar

Conservar junto a los campos sus validaciones e instrucciones. Mantener también
cargas, vacíos, errores persistentes de consulta, avisos de seguridad, integridad,
bloqueadores y acciones de recuperación. Un Toast es información complementaria:
los datos guardados, el estado de la cuenta o el historial siguen disponibles
fuera de la notificación. Los resultados operativos ya no se repiten como avisos
de éxito bajo los formularios.

No hay dependencias adicionales ni cambios en autenticación, rutas o contratos API.

## Archivos de la integración

Rutas relativas a `frontend/src/app/`:

| Área | Archivos nuevos o modificados |
| --- | --- |
| Infraestructura | `shared/notifications/notification.service.ts`, `notification.service.spec.ts`, `toast-container.component.ts`, `toast-container.component.html`, `toast-container.component.scss`, `toast-container.component.spec.ts`, `README.md` (todos en esta carpeta) |
| Layout | `core/layout/app-layout.component.ts`, `.html`, `.spec.ts` |
| Acceso | `features/access/pages/identity-admin.page.ts`, `.html`, `.spec.ts`; `conflict-exceptions.page.ts`, `.html`, `.spec.ts`; `features/access/ui/assignment-form.component.ts` |
| Carreras | `features/careers/pages/career-catalog.page.ts`, `.html`, `.spec.ts` |
| Procesos | `features/processes/pages/process-create.page.ts`, `process-planning.page.ts`; `features/processes/ui/process-assignment-form.component.ts`, `process-state-controls.component.ts`, `process-state-controls.component.spec.ts` |
| Modelos | `features/models/pages/model-editor.page.ts`, `model-history.page.ts`; `features/models/ui/publication-panel.component.ts` |
| Evidencias | `features/evidence/pages/evidence-editor.page.ts`, `.spec.ts`, `evidence-version.page.ts` |
| Revisión | `features/reviews/pages/document-review-inbox.page.ts` |
| Evaluación | `features/evaluations/pages/indicator-evaluation.page.ts`, `quantitative-evaluation.page.ts`, `academic-disagreement.page.ts` |
| Seguimiento documental | `features/reporting/pages/documentary-history.page.ts`, `documentary-closure.page.ts` |
| Informes | `features/reports/pages/report-workflow.page.ts` |
| Mejora | `features/improvement/pages/improvement-plan.page.ts` |
| Archivo | `features/archive/pages/process-archive.page.ts` |

Las pruebas de los módulos migrados ahora verifican el resultado en el servicio
compartido en lugar de buscar el antiguo aviso bajo el formulario, conservando
las comprobaciones de payload, permisos, revisiones, valores y validación.
