# AA5 — Corte de trazabilidad del backend

“Cierre” significa corte documental con estados explícitos; no aceptación del incremento ni resolución artificial de pendientes. Línea base: matriz AA2 de24 filas, procesos/C4/contratos APE3, ADR AA3, amenazas/SEC y verificaciones VT de Semana4, despliegue propuesto APE5. No se cambian requisitos aprobados por escribir esta tabla.

| Requisito / AC | Arquitectura e implementación | Contrato / operación | Seguridad y prueba | Despliegue / evidencia | Estado al corte |
|---|---|---|---|---|---|
| AA-RF-01/02/03; AC-AA-01–04 | Identidad, SessionController/SessionService | sessions POST/DELETE; me GET | T-01; VT-01–03/12; SessionServiceTest; APE3 login/logout | Spring/SQLite histórico; sin runtime nuevo | Parcial; errores/expiración/identidad y stack exigido requieren verificar. |
| AA-RF-04; AC-AA-05/06/13/24 | ScopeAuthorizationService y servicios de objeto | Rutas protegidas catálogo/procesos | T-02; VT-04/05; cuentas/ámbitos sintéticos | Sin autorización de producción observada | Parcial por fuente/evidencia previa; no todo objeto cubierto. |
| AA-RF-05/06/07; AC-AA-07–13/27 | CareerController/servicio/repositorio/dominio | units/careers POST/GET y páginas | VT-07; APE3 alta/duplicado/lista; fuente valida unidad/fechas | PostgreSQL principal no aceptado en campaña actual | Parcial; variantes de fecha/unidad, lector/total y DRF pendientes. |
| AA-RF-08/09; AC-AA-14–17 | Revisiones/historial/estado de catálogo | PATCH, history, state-decisions | T-03; VT-06/07; APE3 ETag/historia/desactivación | Sin transacciones simultáneas/restore probado | Parcial; protección presente, regresión completa pendiente. |
| AA-RF-10; AC-AA-18/22 | ModelCatalogPort y servicio de modelos | Modelo legado/configurado | APE3 HTTP-16/16B, 409/200 | Sin nueva publicación normativa | Parcial; fixtures no acreditan vigencia institucional. |
| AA-RF-11/12; AC-AA-19–24 | SelfAssessmentService; PREPARATION | processes POST/GET y detalle | T-02/05; SEC-01; VT-04/08 | Evidencia previa solo SQLite | Parcial; límites/ámbitos/negativos y principalDB pendientes. |
| AA-RF-13; AC-AA-25 | AuditPort y transacciones | Mutaciones/consulta autorizada | T-06; VT-11; test fuente | Sin inyección de fallo ni restauración | Pendiente de verificación del caso crítico de auditoría. |
| AA-RF-14; AC-AA-26; AA-RNF-10 | Handlers/writer y ADR-AA3-001 propuesto | Problem Details actual vs sobre objetivo | T-09; SEC-04; VT-01/12 | Docs Swagger deshabilitadas en prod; publicación estática propuesta | Parcial; no sobre universal ni desafío resuelto. |
| AA-RNF-01/02/03/05/06/07/09; VC correspondientes | Identidad, permisos, concurrencia, auditoría/límites de capas | Seguridad transversal | Matriz T/SEC/VT y pruebas fuente | Perfil/env/imagen propuestos no validados | Parcial; no certificación global ni parche de seguridad nuevo. |
| AA-RNF-04/08; VC-AA-04/08 | Frontend y recorridos, fuera de nueva campaña | Rendimiento/accesibilidad | Sin carga ni auditoría WCAG nueva | Sin medición de entorno principal | Pendiente; no extrapolar framework ni tiempos de HTTP previos. |
| Guías Semana4: seguridad funcional | Tests pytest propuestos; siete casos | Fixture loopback, no DRF | Gate omitido no cuenta como aprobado; VT por ejecutar | Sandbox no establecido | Pendiente de implementación exigida, ejecución y CF-04-01. |
| Guías Semana5: despliegue/portabilidad | Dockerfile/Compose/contexto/env propuestos | Contrato aceptado por confirmar | TLS/mínimo privilegio/DB/files por validar | Sin build/imagen/URL/publicación | Preparado documentalmente, no desplegado. |
| Participación/feedback/versionado | Notas y bitácora | Guías ACD/APE/AA | CRUZADA-04-01/FEEDBACK-05-01 | Sin commits/PR de estas entregas | Pendiente de evidencia humana verificable. |

Para convertir una fila en verificada: agregar prueba exacta y resultado, commit/artefacto, entorno, fecha, responsable y límites. Para aceptarla: registrar decisión humana y criterios cubiertos. No cerrar por existir un archivo, hash, pipeline o esquema sintácticamente válido.
