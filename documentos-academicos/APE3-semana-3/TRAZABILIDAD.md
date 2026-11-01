# APE3 — Trazabilidad de especificación, arquitectura, contrato e implementación

Origen: [dossier AA2](../AA2-semana-2/DOSSIER_REQUISITOS_AA2.md), [matriz AA2](../AA2-semana-2/MATRIZ_TRAZABILIDAD.md), [specification APE2](../APE2-semana-2/specification.md) y flujos F-ACD3-01/02. Los criterios citados mantienen su redacción; una prueba parcial no los redefine ni los valida por completo. Los RF de esta tabla son AA-RF y los RNF, AA-RNF.

Evidencia nueva: `evidencias/http-resultados.json`, `esquemas-resultados.json`, `pruebas-maven.json`. Los HTTP se refieren a sus campos `case`. Estado **parcial** significa escenario comprobado en Spring/SQLite, sin certificación de todo el requisito ni de cumplimiento DRF.

| Necesidad / actor / historia | Requisito y AC/VC | Proceso y componente | Operación del OpenAPI observado | Evidencia nueva | Estado / vacío |
|---|---|---|---|---|---|
| N-AA-01 / A-AA-01 / HU-AA-01 | AA-RF-01; AC-AA-01/02 | Acceso; SessionController/SessionService | POST /sessions | HTTP-02, esquema de SessionView | Válido comprobado; credencial errónea/no enumeración y límite de intentos sin ejecutar. |
| N-AA-01 / A-AA-01 / HU-AA-01 | AA-RF-02; AC-AA-03 | Acceso; SessionService | DELETE /sessions y GET protegido | HTTP-20/21 | 204 sin cuerpo y acceso revocado comprobados; no inspección directa del token persistido. |
| N-AA-01 / A-AA-01 / HU-AA-01 | AA-RF-03; AC-AA-04 | Identidad propia | GET /me, existente fuera del subconjunto APE3 | Ninguna nueva | Pendiente; no ampliar cobertura por login. |
| N-AA-01 / A-AA-01–04 / HU-AA-01–06 | AA-RF-04; AC-AA-05/06/13/24 | Autorización por ámbito y CSRF | Operaciones protegidas | HTTP-01/04/21 | Falta de sesión y CSRF comprobados; capacidad funcional, lector ajeno, totales ocultos y proceso ajeno pendientes. |
| N-AA-02 / A-AA-02 / HU-AA-02 | AA-RF-05; AC-AA-07/27 | F-ACD3-01; CareerAdministrationService | POST /academic-units | HTTP-03 | Alta comprobada; no consulta posterior de unidad ni duplicado de unidad. |
| N-AA-02 / A-AA-02 / HU-AA-02 | AA-RF-06; AC-AA-08–12 | F-ACD3-01; CareerController/Career | POST /careers | HTTP-05/06/07 | Alta, DTO incompleto y duplicado normalizado; unidad inválida/fechas invertidas pendientes. |
| N-AA-04 / A-AA-02/04 / HU-AA-03 | AA-RF-07; AC-AA-13/14 | F-ACD3-01; servicio/repositorio | GET /careers, /careers/{careerId}, /history | HTTP-08/12/13 | Detalle, lista filtrada e historial; no aislamiento de lector ni exhaustividad de paginación. |
| N-AA-05 / A-AA-02 / HU-AA-04 | AA-RF-08; AC-AA-14/15 | F-ACD3-01; servicio y revisiones | PATCH /careers/{careerId} | HTTP-09/10/11/12 | Revisión nueva, historial y rechazo de revisión vieja; simulado secuencialmente, no dos transacciones concurrentes ni ambos actores. |
| N-AA-02/03 / A-AA-02/03 / HU-AA-05/06 | AA-RF-09; AC-AA-16/17/20 | F-ACD3-01/02; estado de carrera | POST /careers/{careerId}/state-decisions | HTTP-15 | Cambio a DISABLED/revisión 3 comprobado; bloqueo de proceso posterior y repetición de estado no ejecutados aquí. |
| N-AA-03 / A-AA-03 / HU-AA-06 | AA-RF-10; AC-AA-18/22 | F-ACD3-02; EvaluationModelService/ModelCatalogPort | GET versión legado y configurada | HTTP-16/16B | Identidad y PUBLISHED comprobados en configurada; 409 legado detectado. Versión no publicada para alta pendiente. |
| N-AA-03 / A-AA-03 / HU-AA-06 | AA-RF-11; AC-AA-19–22 | F-ACD3-02; SelfAssessmentService | POST /processes | HTTP-17 | PREPARATION/revisión 0 con fixture autorizado; rechazos de vigencia/modelo/carrera pendientes. |
| N-AA-04 / A-AA-03 / HU-AA-06 | AA-RF-12; AC-AA-23/24 | F-ACD3-02; servicio de proceso | GET /processes y /processes/{processId} | HTTP-18/19 | Consulta propia y presencia en arreglo; actor ajeno pendiente. |
| N-AA-05 / A-AA-02/03 / historias de cambio | AA-RF-13; AC-AA-25 | Ambos; AuditPort/SelfAssessmentAuditService | Mutaciones | Código revisado; sin fallo de auditoría inducido | Pendiente de ensayo de atomicidad/auditoría; no confundir 201 con cumplimiento del caso de fallo. |
| N-AA-05 / A-AA-01–04 / historias transversales | AA-RF-14; AC-AA-26 | Handlers/CorrelationFilter | Rechazos de catálogo/modelo | HTTP-01/04/05/07/09/11/14/16; Ajv | Cuerpo conforme en muestras; revisión sistemática de secretos y todas las rutas pendiente. |
| N-AA-01 / usuarios | AA-RNF-01/02/03; VC-AA-01–03 | Identidad/acceso | Sesión y respuestas | Redacción de tokens de evidencia | Redactar artefactos no prueba hash, expiración, privacidad del sistema o ausencia de secretos en logs. |
| N-AA-04 / usuarios | AA-RNF-04; VC-AA-04 | Recorridos completos | Consultas | Duraciones por solicitud en JSON | No son p95 visible de navegador, campaña de carga ni medición del RNF. Pendiente. |
| N-AA-05 / A-AA-02/03 | AA-RNF-05; VC-AA-05 | Revisiones | PATCH /careers/{careerId} | HTTP-09–12 | Precondición comprobada; simultaneidad y actores reales de VC pendientes. |
| N-AA-05 / auditor/soporte autorizado | AA-RNF-06/07; VC-AA-06/07 | Auditoría/correlación | Transversal | correlationId en errores de muestra | No se verificó inmutabilidad ni correspondencia con logs de éxito/error. Parcial. |
| N-AA-01/04 / usuarios | AA-RNF-08; VC-AA-08 | Frontend | Fuera de prueba HTTP | Ninguna nueva | Accesibilidad pendiente, sin inferencia desde framework. |
| N-AA-05 / equipo | AA-RNF-09; VC-AA-09 | Límites C3 y reglas | Código seleccionado | 13 ModuleArchitectureTest y 10 tests de catálogo | Dependencias y pruebas seleccionadas correctas; SQL y mantenibilidad completa no certificados. |
| N-AA-04/05 / cliente | AA-RNF-10; VC-AA-10 | Contrato/filtros/handlers | 14 operaciones seleccionadas | Redocly; 22 respuestas Ajv; headers HTTP | Muestras comprobadas; sobre objetivo sin implementación. 401 observado sin WWW-Authenticate: HTTP-AA-01 sigue abierto. |

La matriz mantiene la cadena problema/necesidad→actor→RF/HU→AC→proceso→C3→operación→código→evidencia. Los nombres concretos y rutas de implementación se encuentran en [FUENTES.md](FUENTES.md), y UML enlaza las clases principales. No se marca ningún requisito como validado institucionalmente.
