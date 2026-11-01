# ACD 4 — Modelo de amenazas y revisión de seguridad

Semana 4 · Actividad grupal con evidencia individual · SIGEAC / SEC-UNL.

Integrantes y fecha de sesión pendientes. Asignatura/docente se toman como referencia de APE4, Desarrollo Basado en Plataformas / Edison Leonardo Coronel Romero, sujetos a confirmar para esta ACD. No se declara una sesión ni revisión cruzada realizada.

## 1. Incremento, activos y fronteras

Se revisan los dos flujos de Semana 3: catálogo con historial y preparación/consulta de proceso, con identidad, autorización y auditoría como dependencias. Requisitos: AA-RF-01–14, AA-RNF-01–03/05–07/09/10 y los AC/VC del dossier AA2. C4 y OpenAPI provienen de APE3. El backend real es Spring Boot, no DRF; sobre universal y decisión tecnológica siguen pendientes.

La [matriz de amenazas](../soporte-semanas-4-5/MATRIZ_AMENAZAS.md) define ocho activos A-01–08, seis fronteras F-01–06, ocho amenazas T-01–08 y una observación protocolaria T-09, razones de impacto y permisos. Debe leerse como parte de este informe. Protege cuentas/credenciales, sesión/CSRF, catálogo, procesos, auditoría, configuración, recursos y artefactos. Los documentos/evidencias del producto completo quedan fuera de los endpoints seleccionados; no se presupone inexistencia global ni ausencia de riesgo en ellos.

Superficies: POST sessions, rutas protegidas de carreras/unidades/procesos/modelos, UUID/path, filtros de búsqueda/página, JSON, cookies, CSRF, If-Match/ETag, configuración env y DB. Swagger aparece permitido por el filtro, pero el perfil prod deshabilita sus endpoints: no se acusa exposición productiva a partir de una regla aislada. Recuperación de cuenta, uploads y servicios externos no se fuerzan en este modelo sin operación del recorte que los justifique.

Personas sin sesión no obtienen recursos protegidos; lectores tienen ámbito; administración de catálogo no concede gestión de proceso ni autoridad institucional. Las cuentas técnicas se rechazan para operaciones funcionales del servicio. La matriz separa quién puede hacer qué sobre cada recurso; no trata “autenticado” como un rol universal.

## 2. Revisión de controles y amenazas

### Autenticación

SessionService normaliza usuario, compara BCrypt y usa hash ficticio para desconocido, registra intentos, emite 32 bytes aleatorios para token/CSRF y persiste hashes SHA-256. Revalida identidad activa, revocación y expiración/inactividad; logout revoca. SessionCookie usa HttpOnly, SameSite=Lax y Secure configurable. Por fuente existe una limitación por sujeto en 15 minutos; no se asume que registrar sourceKey implemente un limitador global.

SEC-02 registra la discrepancia “consecutivos” frente al conteo acumulado de fallos de la ventana. Primero confirmar política; después cambiar requisito o código con prueba de secuencia fallo-éxito-fallo. No inventar un bypass a partir de esa diferencia. Credencial válida determina identidad, no competencias ni acceso a una carrera.

### Autorización

ScopeAuthorizationService comprueba usuario activo no técnico, asignación vigente, scope que contiene el solicitado, capacidad y conflicto no exceptuado. CareerAdministrationService distingue administración global y lectura por carrera, incluidos totales e historial; SelfAssessmentService evalúa PROCESS_MANAGE por carrera y visibilidad al consultar. Revisar cada ruta: UUID conocido no es permiso. Los candidatos de asignaciones/auditoría del antecedente securityOutputs son needs_validation y no se dan por resueltos o confirmados aquí.

### Validación sintáctica y de negocio

| Operación | Sintaxis/estructura | Negocio y protección | Resultado relevante |
|---|---|---|---|
| POST sessions | username/password obligatorios con longitudes DTO | Identidad habilitada, credencial, ventana de intentos | 201 / 400 / 401 / 429 según caso. |
| POST careers | code/name/UUID/validFrom; límites 100/300; validUntil opcional | Unidad activa, código normalizado único, vigencia coherente, CAREER_ADMIN, CSRF | 201 / 400 / 403 / 409 / 422. |
| PATCH careers/{id} | UUID, cuerpo completo editable e If-Match fuerte numérico | Revisión vigente, unidad/código válidos, capacidad y auditoría | 200 / 400 / 412 / 428 además de rechazos de ámbito/negocio. |
| POST processes | Referencias y fechas con tipos/obligatoriedad | Career/unidad activas, período dentro de vigencia, versión publicada y scope | 201 PREPARATION; 400/403/404/409/422 según causa. |
| GET careers | Filtros/página dentro de límites | Visibilidad y total autorizado | 200 o rechazo; no mezclar página válida con permiso a todos. |

Una fecha con formato válido puede estar fuera de la vigencia. Una relación UUID bien formada puede no existir o no estar autorizada. Los DTO de catálogo rechazan campos extra; no extender esa afirmación automáticamente a todo comando. No se declaran pruebas de archivos del incremento cuando no se seleccionaron uploads.

### Errores, secretos y configuración

Handlers/writer retornan Problem Details con código y correlación; errores inesperados tienen descripción genérica y registro técnico. Las muestras anteriores muestran 400/401/403/404/409/412/428 y 204 sin cuerpo, pero no todas las rutas/500. No se certifica ausencia de secretos en todos los logs. El sobre académico aún no se aplica; eso es diferencia contractual, no prueba de vulnerabilidad explotable. SEC-04 identifica el desafío401 ausente sin inventar Bearer.

Se inspeccionaron `.gitignore`, ejemplo env, prod y workflow manual; valores de ejemplo/CI sintéticos no se clasifican como credenciales reales. El escaneo confiable de 1.091 archivos de texto rastreados, acotado por tamaño/exclusiones, buscó tres formatos de credencial y retuvo solo metadatos: cero coincidencias. Se revisaron nombres históricos .env/pem/key, sin coincidencias; no se escaneó todo el contenido histórico ni formatos arbitrarios. No abrir ni copiar archivos personales de credenciales para llenar evidencia.

### Mitigación y priorización

Cada amenaza tiene control, P/I, mitigación y prioridad. El análisis AA4 utiliza exactamente la matriz de la guía: 1–2 Bajo, 3–4 Medio, 6 Alto, 9 Crítico. El modelo incluye escenarios con controles presentes, no solo debilidades. No hay nuevas vulnerabilidades confirmadas ni se asigna severidad de vulnerabilidad a needs_validation. Los SEC son observaciones fuente o riesgos potenciales documentados en [hallazgos y trazabilidad](../soporte-semanas-4-5/HALLAZGOS_Y_TRAZABILIDAD.md).

## 3. Clínica, evidencias y ajustes

| Actividad ACD4 | Desarrollo / evidencia |
|---|---|
| 1 Revisar incremento | Sección 1, fuentes F-01–12 y límites. |
| 2 Identificar activos | Matriz compartida A-01–08 y razones/impacto/acceso. |
| 3 Actores/confianza | Fronteras y matriz por capacidad/ámbito. |
| 4 Superficies | Sesión, rutas, URL/query/JSON/headers, configuración y DB; exclusiones explícitas. |
| 5 STRIDE | T-01–09 pertinentes, sin forzar una categoría en una ruta inexistente. |
| 6 Matriz | Activo, causa/condición, P/I, riesgo de escenario y controles. |
| 7 Autenticación | Sesión/hash/expiración/invalidación y política pendiente. |
| 8 Autorización | Matriz de operaciones y scopes; lectura/administración no equivalentes. |
| 9 Entradas | Tabla sintaxis frente a negocio con fuentes concretas. |
| 10 Errores | Estados, Problem Details, correlación y vacío del sobre. |
| 11 Secretos/config | Metadatos de escaneo, configuración y límites de historia. |
| 12 Riesgos API | BOLA potencial, volumen/listados, permisos y configuración. |
| 13 Mitigaciones | Una o más por amenaza; ubicación/prueba en SEC/VT. |
| 14 Prioridad | Justificación diferenciada, sin todos altos/críticos. |
| 15 Revisión cruzada | CRUZADA-04-01 preparada; falta otro integrante/equipo y observación auténtica. |
| 16 Ajustes | CF-04-01 y trazabilidad preparados; no se inventa parche ejecutado. |

Guion de socialización: mostrar C4/DFD y activos; elegir T-02 y T-05; seguir endpoint→control→test; explicar autenticación/autorización; revisar SEC-02; pedir observación sobre scope, lista/total y límites; registrar responsable/fecha/decisión/cambio. Una revisión asistida no equivale a revisión cruzada humana.

La auditoría formal queda **incompleta**: falta sandbox OS completo, no hay Docker/Podman ni límites de ejecución acreditados. Sus metadatos y vacíos están en `evidencias/auditoria/`. La habilidad exige no ejecutar target sin esas condiciones. Solo se revisaron fuentes y evidencia anterior, y se generaron metadatos con código confiable del analista. No se corrieron nuevos tests funcionales ni se probaron servicios ajenos.

Evidencia grupal: informe, matrices compartidas, fuentes, plan VT y trazabilidad. Evidencia individual: [NOTA_INDIVIDUAL.md](NOTA_INDIVIDUAL.md), a completar con revisión y observación reales. No hay aceptación docente, commits ni corrección de seguridad simulados.
