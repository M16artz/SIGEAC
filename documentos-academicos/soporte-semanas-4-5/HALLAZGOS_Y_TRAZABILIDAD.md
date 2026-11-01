# Hallazgos académicos, controles y trazabilidad — Semanas 4–5

Se diferencia **observación fuente comprobada**, **evidencia histórica de ejecución**, **riesgo potencial** y **vulnerabilidad confirmada mediante reproducción**. Esta revisión no establece nuevas vulnerabilidades confirmadas. Los puntajes siguientes pertenecen a escenarios de planificación AA4 y no son CVSS ni severidades de registros needs_validation.

| ID | Activo / amenaza / causa | Evidencia concreta | Estado de certeza | P / I / resultado / nivel de escenario | Control actual | Acción, ubicación y comprobación |
|---|---|---|---|---|---|---|
| SEC-01 | A-07 / T-05; cargar procesos completos antes de filtrar | JdbcSelfAssessmentRepository.list selecciona sin LIMIT; SelfAssessmentService.list filtra luego | Mecanismo fuente comprobado; agotamiento potencial, no reproducido | 2 / 2 / 4 Medio: requiere volumen, afecta disponibilidad del listado | Autenticación y filtrado de visibilidad, sin paginación declarada | Cambiar servicio/repositorio/contrato y cliente conjuntamente; paginar en DB conservando scope/total; VT-08, límite y prueba de ámbito. |
| SEC-02 | A-01 / T-01; política de intentos no coincide literalmente con RNF-003 | RNF-003 dice consecutivos; countFailures cuenta todos los fallos en ventana y no recorta tras éxito | Discrepancia fuente comprobada; efecto de política requiere confirmación y prueba | 2 / 2 / 4 Medio: secuencia razonable, puede limitar acceso legítimo; no es bypass probado | Limitación por sujeto, auditoría y errores uniformes | Confirmar acumulados vs consecutivos; después corregir requisito o consulta/servicio y test fallo-éxito-fallo. No cambiar política unilateralmente. VT-03. |
| SEC-03 | A-02/06 / T-04; transportar sesión sin condiciones de producción | application.yml local false; application-prod.yml true; SessionCookie usa propiedad | Riesgo condicionado a despliegue; no exposición productiva comprobada | 1 / 3 / 3 Medio: exige configuración equivocada, comprometería sesión | Perfil prod y flag Secure, HttpOnly/SameSite=Lax | Verificar configuración efectiva, TLS/proxy y cookie en entorno aislado; rechazo de demo/credenciales reales en imagen. VT-09. |
| SEC-04 | A-08 / T-09; 401 sin WWW-Authenticate | AccessProblemWriter no lo establece; APE3 HTTP-01/21 no lo registró | Observación de protocolo apoyada por fuente/muestra previa; no explotación | 1 / 1 / 1 Bajo: impacto observado de interoperabilidad, sin bypass | 401 y cuerpo seguro/correlación | Resolver desafío compatible con mecanismo elegido; probar header y no enumeración VT-01/02. No añadir JWT/Bearer por conveniencia. |
| SEC-05 | A-01/07 / T-08; admisión distribuida de intentos costosos | SessionService cuenta por subject y calcula BCrypt; sourceKey registrado no equivale a throttling | Riesgo potencial; controles de proxy/operación no observados | 2 / 2 / 4 Medio: múltiples sujetos pueden aumentar coste, sin DoS demostrado | Límite por cuenta y ventana; hash ficticio evita respuesta diferencial simple | Propietario debe confirmar límites globales; ensayo acotado con límites de CPU/tiempo, sin inundación. VT-10. |

La prioridad de ejecución de controles de identidad T-01 es alta por el activo, aunque SEC-02/05 no sean vulnerabilidades explotadas confirmadas. No se clasifica todo como alto/crítico. Los niveles 1–2 Bajo, 3–4 Medio, 6 Alto y 9 Crítico se aplican exactamente como AA4.

## Matriz SDD

| Amenaza / hallazgo | Requisito / criterio previo | Componente / endpoint | Control | Prueba / evidencia | Corrección y estado |
|---|---|---|---|---|---|
| T-01 / SEC-02/05 | AA-RF-01; AA-RNF-01/02; RNF-003; AC-AA-01/02 | Identidad; POST sessions | BCrypt, ventana, sesión aleatoria revocable | SessionServiceTest existe; APE3 HTTP-02; VT-01–03 planificados | Confirmación de política y validación pendientes. |
| T-02 | AA-RF-04/07/12; AC-AA-06/13/24 | Catálogo/procesos; detalle/listados/mutación | ScopeAuthorizationService, chequeo por objeto | CareerAdministrationServiceTest; VT-04/05 pendientes de nueva ejecución | Sin nueva omisión confirmada en este recorte; mantener regresión. |
| T-03 | AA-RF-08; AA-RNF-05; AC-AA-14/15 | PATCH careers | CSRF y If-Match | APE3 HTTP-04/09–12; VT-06/07 | Revisión secuencial previa; prueba concurrente pendiente. |
| T-04/07 / SEC-03 | AA-RNF-03; RNF-005; VC-AA-03 | Configuración/cookie/despliegue | prod, env, .gitignore | Escaneo acotado y fuente; VT-09 | Sin secreto real confirmado; TLS y entorno efectivo no comprobados. |
| T-05 / SEC-01 | AA-RNF-04/10; VC-AA-04/10 | GET processes, repositorio | Filtro de ámbito; límites pendientes | Fuente; VT-08 | Cambio compatible de paginación planificado, no implementado. |
| T-06 | AA-RF-13; AA-RNF-06/07; AC-AA-25 | Auditoría y mutaciones | Transacción y registro | SelfAssessmentAuditServiceTest existe; VT-11 | Inyección de fallo no ejecutada. |
| T-09 / SEC-04 | AA-RF-14; AA-RNF-10; AC-AA-26; HTTP-AA-01 | Handlers/writer; respuestas 400/401/403/404/409/500 | Problem Details, código/correlación | APE3 JSON y VT-01/12 | Contrato universal y desafío pendientes; AA3 ADR propuesto, no aplicado. |

## Inventario de verificación a ejecutar en sandbox conforme

| ID | Caso y resultado que debe comprobarse | Fuente o prueba existente | Estado actual |
|---|---|---|---|
| VT-01 | Sesión ausente: 401, cuerpo seguro, desafío válido según decisión | SecurityConfiguration/AccessProblemWriter; prueba académica propuesta | Muestra previa 401; desafío ausente; nueva regresión sin ejecutar. |
| VT-02 | Credenciales válidas/incorrectas/desconocidas: éxito solo válido y fallo uniforme | SessionServiceTest, unknownAndWrongCredentialsShareTheSameFailureCode | Test fuente existente; no se ejecutó en Semana 4. |
| VT-03 | Expiración antes/después de 30 min, revocación y secuencia de fallos | SessionServiceTest; reloj controlado | No esperar 30 min ni manipular cuentas reales; nueva ejecución pendiente. |
| VT-04 | Lector A no obtiene carrera B ni total/lista ajenos | CareerAdministrationServiceTest y prueba HTTP propuesta | Control fuente y prueba existente; sin nueva reproducción. |
| VT-05 | Lector o cuenta técnica no cambia catálogo | Servicio + ScopeAuthorizationServiceTest | Nueva ejecución pendiente. |
| VT-06 | Mutación autenticada sin CSRF devuelve 403 | APE3 HTTP-04 | Evidencia histórica, no campaña nueva. |
| VT-07 | DTO inválido 400; duplicado 409; If-Match ausente 428/viejo 412; fechas/relaciones inválidas | APE3 casos y CareerControllerTest/CareerTest | Parcial histórico; reglas faltantes pendientes. |
| VT-08 | Página/límite/ámbito/total de procesos y recursos usados | Contrato/servicio/repositorio a refinar | Sin paginación nueva ni carga ejecutada. |
| VT-09 | TLS/cookie efectiva, demo false, secretos externos y ausencia en logs/imagen | Configuración prod y despliegue propuesto | Solo revisión estática; no despliegue observado. |
| VT-10 | Admisión de login con límites globales razonables | Servicio y posible proxy del propietario | Sin load test, sin límite global comprobado. |
| VT-11 | Fallo de auditoría no presenta cambio crítico completado | AuditPort, tests relacionados | Pendiente de fixture con fallo controlado. |
| VT-12 | 204 sin cuerpo, errores/correlación sin secretos; schema real aceptado | APE3 HTTP-20 y errores; OpenAPI/AA3 | Histórico parcial; sobre universal sin implementar. |

## Correcciones y revisión cruzada

No hay corrección de código de seguridad nueva ejecutada y validada en estas entregas. El rechazo 409 de modelo legado de APE3 llevó a corregir un escenario/contrato, pero **no es evidencia de parche de seguridad de APE4**. CF-04-01 queda reservado para una regresión real, cambio y reejecución; no se rellena con un resultado ficticio.

| Registro | Amenaza / observación recibida | Decisión / mitigación / artefacto | Estado |
|---|---|---|---|
| CRUZADA-04-01 | Pendiente de otro integrante/equipo identificado y fecha real | Registrar qué cambia o por qué se mantiene, responsable, requisito/componente/endpoint y commit | No realizada ni atribuida. |
| CF-04-01 | Pendiente de fallo de prueba o hallazgo validado | Registrar antes→parche→después y evidencia reproducible | No implementada/verificada. |

La corrección documental, el escaneo acotado y la preparación de tests/configuración sí están realizados; no sustituyen una mitigación funcional demostrada.
