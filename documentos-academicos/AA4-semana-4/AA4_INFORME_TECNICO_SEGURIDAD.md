# AA 4 — Informe técnico de seguridad y priorización de riesgos

Semana 4 · Actividad Autónoma · 4,5 horas previstas · Individual articulada con SIGEAC / SEC-UNL. Estudiante, revisión personal y fecha real de entrega pendientes. Referencia de asignatura/docente: APE4, por confirmar para esta AA.

## 1. Identificación, alcance y fundamentos

Se analiza el incremento de acceso, catálogo e inicio en PREPARATION de procesos, a partir de ACD4 y APE4. Se protege identidad/credencial/sesión, catálogo e historia, procesos/referencias, auditoría, configuración y recursos de servicio. El [modelo compartido](../soporte-semanas-4-5/MATRIZ_AMENAZAS.md) explica por activo importancia, quién debe acceder e impacto. Debe acompañar el informe, no es una lista externa genérica.

Autenticación pregunta quién actúa: SessionService comprueba credencial, sesión hash, identidad vigente, expiración/revocación. Autorización pregunta qué operación sobre qué recurso: ScopeAuthorizationService combina función/capacidad, vigencia, conflicto y scope. El filtro autenticado no reemplaza la comprobación por carrera/proceso. CAREER_ADMIN no concede autoridad institucional ni gestión automática de procesos.

OWASP API1 y Authorization orientan el análisis por objeto; API4, los límites de consumo; Authentication/Session, intentos y cookie; Input Validation, sintaxis/semántica; Secrets y Error Handling, exposición y diagnóstico. Se consultaron [fuentes oficiales](../soporte-semanas-4-5/FUENTES.md), sin convertir cada categoría en una vulnerabilidad del proyecto. La referencia STRIDE ordena amenazas razonables.

Estado metodológico: análisis fuente, revisión de evidencia previa y preparación de tests. La auditoría formal está incompleta por aislamiento OS no acreditado; no se ejecuta target. APE4 contiene siete tests pytest **no ejecutados**, sin DRF/pytest-django implementados. Esta limitación impide afirmar corrección de hallazgos o incremento seguro terminado.

## 2. Pruebas, hallazgos y priorización

### Resultados que sí existen y su procedencia

APE3 registró 23 tests seleccionados y 22 casos HTTP finales en Spring/SQLite: login, acceso anónimo/CSRF, DTO inválido, alta/duplicado, revisión/historia, modelo legado/configurado, preparación/consulta, logout y rechazo posterior. No son pruebas APE4 nuevas. El intento inicial esperaba 200 de la representación legada y recibió 409; corrigió el escenario/contrato, no un defecto de seguridad.

SessionServiceTest en fuente cubre invalidación, expiración con reloj y credenciales desconocidas/incorrectas; ScopeAuthorizationServiceTest y CareerAdministrationServiceTest cubren permisos. Su presencia no prueba ejecución reciente. APE3 no ejecutó la suite completa de identidad ni todos los VT. Credenciales inválidas, expiración real navegador, lector B, fallo auditoría, carga y prod no están completamente verificados en esta campaña.

Escaneo confiable: 1.091 textos rastreados, tres patrones, cero coincidencias; nombres históricos sensibles, cero. Se excluyeron generados/vendor/tamaños grandes y no se escaneó contenido histórico completo. Es evidencia limitada de revisión, no demostración de “cero secretos”. Valores reales no se retienen ni imprimen. Placeholders y CI sintética se clasifican aparte.

### Hallazgos con identificador

| ID | Activo / amenaza | Vulnerabilidad o causa y evidencia | Control actual | Estado de certeza | Acción y prioridad |
|---|---|---|---|---|---|
| SEC-01 | A-07 / T-05 | GET processes materializa consulta sin LIMIT antes de filtrado; JdbcSelfAssessmentRepository.list y servicio | Autenticación/filtrado de ámbito | Fuente comprobada; agotamiento potencial no reproducido | Planificar paginación/filtrado DB y contrato compatible; VT-08, media. |
| SEC-02 | A-01 / T-01 | RNF-003 dice consecutivos; consulta cuenta acumulados de ventana | Limitación por sujeto y auditoría | Discrepancia documental/lógica; política pendiente, sin bypass demostrado | Confirmar semántica, corregir fuente que proceda y test de secuencia; media. |
| SEC-03 | A-02/06 / T-04 | Local Secure=false frente a prod=true; despliegue efectivo no observado | Perfil prod/HttpOnly/SameSite | Riesgo condicionado, no sesión expuesta acreditada | Verificar TLS/env/perfil/cookie con propietario y fixture; media. |
| SEC-04 | A-08 / T-09 | 401 previo sin WWW-Authenticate y writer sin cabecera explícita | 401 y Problem Details/correlación | Observación protocolaria, no vulnerabilidad explotada | Resolver desafío acorde al mecanismo y regresión; baja. |
| SEC-05 | A-01/07 / T-08 | Límite por sujeto y trabajo BCrypt; control global no observado | Ventana por cuenta y hash ficticio | Riesgo potencial, sin DoS ejecutado | Confirmar admisión/proxy, ensayo acotado; media. |

La [matriz detallada](../soporte-semanas-4-5/HALLAZGOS_Y_TRAZABILIDAD.md) conserva por registro ubicación exacta, control, acción verificable y vínculo RF/AC/VT. Ninguno se presenta como nueva vulnerabilidad confirmada con reproducción independiente. El antecedente securityOutputs/run-1 contiene siete needs_validation de un ámbito más amplio y otro commit; no se eleva a confirmación ni se declara resuelto.

### Escala y cálculo exactos de AA4

P: 1 baja, 2 media, 3 alta; I: 1 bajo, 2 medio, 3 alto. Riesgo de escenario = P×I. Tabla de la guía:

| Probabilidad / impacto | 1 Bajo | 2 Medio | 3 Alto |
|---|---|---|---|
| 3 Alta | 3 Medio | 6 Alto | 9 Crítico |
| 2 Media | 2 Bajo | 4 Medio | 6 Alto |
| 1 Baja | 1 Bajo | 2 Bajo | 3 Medio |

1–2 puede planificarse/aceptarse temporalmente con justificación; 3–4 requiere corrección/seguimiento; 6 antes del cierre salvo justificación excepcional; 9 atención inmediata y flujo no terminado. No se aceptó ningún riesgo por el equipo mediante este documento.

| Hallazgo | P | I | Resultado / nivel de escenario | Justificación |
|---|---:|---:|---|---|
| SEC-01 | 2 | 2 | 4 Medio | Requiere volumen/carga razonable; puede afectar disponibilidad del listado, sin prueba de caída global. |
| SEC-02 | 2 | 2 | 4 Medio | Secuencia fallo-éxito-fallo es posible; una política distinta puede limitar acceso legítimo, sin toma de cuenta observada. |
| SEC-03 | 1 | 3 | 3 Medio | Requiere configuración/transporte incorrectos; una sesión expuesta tendría impacto alto. Prod ya tiene control fuente. |
| SEC-04 | 1 | 1 | 1 Bajo | La consecuencia demostrada es interoperabilidad del desafío, no acceso indebido ni filtración. |
| SEC-05 | 2 | 2 | 4 Medio | Varios sujetos pueden aumentar trabajo; falta medir límites globales/operacionales y agotamiento. |

Los puntajes son valoración de planificación, no CVSS/severidad de vulnerabilidades needs_validation. Se identifica certeza separadamente del impacto hipotético; ninguna clasificación crítica se inventa para completar la tabla. T-01 tiene riesgo inherente alto en el modelo de suplantación por credencial comprometida, pero eso no acredita un bypass local.

## 3. Acciones, trazabilidad y limitaciones

SEC-01: cambiar SelfAssessmentRepository/JDBC/servicio y OpenAPI/cliente de forma compatible para paginar con ámbito/total filtrados; demostrar que no se materializan todos los procesos. SEC-02: confirmar requisito de bloqueo antes de alterar countFailures; si se decide consecutivos, prueba con éxito intermedio; si se decide acumulados, corregir requisito con acuerdo. SEC-03: probar env/perfil efectivo, TLS y cookie en fixture; no inferir configuración productiva. SEC-04: revisar writer/handlers/contrato y desafío del esquema aceptado, sin “solucionar” con Bearer inexistente. SEC-05: confirmar controles de admisión y capacidad antes de proponer límite global, conservando protección por cuenta.

Cada acción debe producir fixture mínimo, resultado antes, parche/revisión, prueba después y regresión relacionada. CF-04-01 permanece pendiente; no se cuenta una corrección documental como parche de seguridad. La [trazabilidad común](../soporte-semanas-4-5/HALLAZGOS_Y_TRAZABILIDAD.md) enlaza amenaza→SEC→RF/RNF→endpoint→control→VT→corrección/estado. Mantiene estados pendientes en vez de alterar specification/ADR/OpenAPI del producto sin decisión.

Limitaciones: análisis de rutas seleccionadas, sin producción, sin sandbox completo, sin nueva campaña HTTP/Maven/pytest, sin aceptación PostgreSQL, sin carga, sin validación de TLS/CORS, sin revisión exhaustiva de logs/cadena de dependencias o contenido histórico. Los tests de cliente propuestos no verifican DRF ni son prueba del aislamiento que ellos mismos necesitan. Los SVG de documentación de APE3 no equivalen a pruebas del backend.

Cobertura de las once actividades: fuentes oficiales (1), activos (2), autenticación/autorización (3), T/SEC (4), revisión de pruebas (5), registros SEC (6), evidencias/certeza (7), cálculo P×I (8), acciones localizadas (9), limitaciones (10), trazabilidad y estados (11). La entrega individual necesita identidad, revisión personal, correcciones propias y enlace real; [NOTA_INDIVIDUAL.md](NOTA_INDIVIDUAL.md) evita atribuir aportes ficticios.

Tiempo de guía: 4,5 h, planificación sugerida 45 min contexto/activos, 45 fuentes, 60 pruebas/hallazgos, 45 prioridad, 45 medidas/trazabilidad y 30 revisión: 270 min, no cronometraje ejecutado. Se utilizó ChatGPT/Codex para investigación/organización y verificaciones estáticas de artefactos; no se declara supervisión/aceptación humana concluida ni práctica funcional cerrada.
