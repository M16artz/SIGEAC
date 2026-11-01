# APE 4 — Implementación de seguridad, validaciones y pruebas de API

## Datos de Identificación del Estudiante y la Práctica

| Campo | Valor |
|---|---|
| Estudiante(s) | Pendiente de integrantes reales. |
| Asignatura / ciclo / unidad | Desarrollo Basado en Plataformas / 5 A / 1. |
| Resultado de aprendizaje | Discute cómo los estándares Web influyen en el desarrollo de software, bajo los principios de solidaridad, transparencia, responsabilidad y honestidad. |
| Práctica / docente | 004 / Edison Leonardo Coronel Romero. |
| Fecha / horario / lugar | Semana 4; fecha real pendiente / por definir según horario académico / aula o laboratorio asignado. |
| Tiempo previsto | 3 horas, no duración medida de trabajo humano. |
| Estado | Revisión de controles existentes y preparación de pruebas. Sin nueva implementación/parche de aplicación, ejecución funcional ni aprobación docente. |

## Objetivo(s) de la Práctica

- Implementar mecanismos de autenticación y autorización coherentes con los actores, roles y recursos del proyecto.
- Aplicar validaciones de entrada, reglas de negocio y manejo seguro de errores.
- Verificar la protección de datos y secretos mediante prácticas básicas de seguridad.
- Implementar y ejecutar pruebas reproducibles de API, integración y controles de acceso.
- Corregir hallazgos identificados durante la revisión de seguridad y mantener trazabilidad con requisitos y amenazas.

La documentación no convierte controles Spring previos en implementación nueva de DRF. Los objetivos de ejecución/corrección funcional no se dan por cumplidos.

## Materiales, Reactivos, Equipos y Herramientas

Insumos: modelo/matriz ACD4 y fuentes compartidas, requisitos/AC AA2, OpenAPI y evidencia APE3, pruebas existentes de identidad/catálogo. Stack real: Java/Spring/PostgreSQL, SQLite auxiliar anterior; la guía exige Python/Django/DRF y pytest-django, no implementados/ejecutados aquí. Se prepara un cliente pytest, que no es un backend Django.

Variables de entorno son mecanismo previsto de secretos; placeholders no son valores reales. Git/GitHub existen, GitFlow y contribución están pendientes. Actions del repositorio usa workflow_dispatch, no se activa desde esta entrega. No hay Docker/Podman ni un sandbox conforme acreditado; WSL disponible no basta para afirmarlo. No se instalan dependencias ni se ejecuta target fuera del aislamiento exigido.

## Procedimiento / Metodología Ejecutada

Inicio: se seleccionan sesión, catálogo y preparación de proceso; se relacionan actores/capacidades, T-01–09, SEC-01–05 y AC existentes. ACD4 no tiene aún retroalimentación humana real. La revisión es estática y usa evidencia histórica, no una nueva campaña local sobre servicio compartido.

| Paso | Trabajo efectivamente preparado/revisado | Estado de cumplimiento |
|---|---|---|
| 1 Autenticación | SessionService, BCrypt, hashes, cookie, expiración/revocación y ventana; VT-01–03 | Implementado previamente en Spring; nueva ejecución y DRF pendientes. |
| 2 Autorización | ScopeAuthorizationService, CAREER_ADMIN/READ, PROCESS_MANAGE; fixtures lector A/carrera B | Fuente y matriz; prueba funcional nueva pendiente. |
| 3 Validaciones | DTO, sintaxis, UUID, fechas, relaciones, duplicado y ETag; VT-07 | Controles previos inspeccionados, sin cambio de negocio. |
| 4 Datos/secretos | Escaneo acotado de texto y nombres históricos, env/prod/.gitignore; reporte sin valores | Comprobación documental limitada, no certificación de ausencia total. |
| 5 Errores | Problem Details, estados y correlación; desafío401/sobre pendientes | No se corrige protocolo ni se migra contrato por documentación. |
| 6 Tests | Siete tests pytest acotados en `pruebas/test_seguridad_api.py`; matriz VT-01–12 | Código de prueba propuesto; no ejecutado. Sin gate se omite y no cuenta como aprobado. |
| 7 Verificación segura | Requisitos de sandbox y datos sintéticos en PLAN_PRUEBAS | No se ejecuta target por falta de aislamiento completo. No servicios ajenos. |
| 8 Correcciones | Acciones SEC concretas y CF-04-01 reservado | Sin parche/regresión ejecutada; no hay antes→después de seguridad acreditado. |
| 9 Trazabilidad | Amenaza→SEC→RF/RNF→endpoint→control→VT→estado | Matriz elaborada, código/contrato de producto sin modificar. |

Los tests propuestos incluyen ausencia de credencial, desafío401, identidad desconocida sintética, falta de CSRF, campos obligatorios, objeto ajeno y logout. El escenario 401 con desafío debe servir de regresión cuando se acuerde un mecanismo compatible; no se fuerza JWT. El plan limita red/host/tamaño/respuestas y datos, pero ese gate no es la sandbox OS.

Para expiración se selecciona test existente con reloj controlado, no se espera media hora ni se alteran cuentas ajenas. Para rollback de auditoría y concurrencia se requiere fixture dedicado; no se extrapolan las aserciones secuenciales de APE3.

Cierre: falta demostrar flujo protegido al docente, corrección de seguridad reproducida y contribución versionada real. Un test propuesto que se sabe que necesita corrección no es un fallo observado nuevo. El 409 de modelo de APE3 corrigió el escenario de representación, no una vulnerabilidad APE4.

## Resultados

| Resultado esperado | Resultado de esta entrega | Pendiente |
|---|---|---|
| Autenticación/autorización implementadas y verificadas | Código Spring existente inspeccionado y pruebas fuente localizadas | DRF/decisión tecnológica y ejecución en sandbox. |
| Validaciones y negocio | Matriz sintaxis/negocio y controles por operación | Ejecutar variantes VT y corregir si fallan. |
| Secretos fuera del código | Configuración externa y escaneo acotado sin coincidencias de tres patrones | Formatos no cubiertos, historia completa/rotación si hay indicio. |
| Errores/API | Formato actual y diferencias trazados | Sobre universal/desafío y pruebas aceptadas. |
| Pruebas reproducibles | Cliente pytest propuesto, PLAN_PRUEBAS y VT | Dependencias, sandbox, fixtures, ejecución/resultados. |
| Correcciones | Acciones/localización/verificación definidas | Ningún parche funcional nuevo demostrado. |
| Trazabilidad | Matrices compartidas y relación con AC | Añadir commit/resultados cuando existan. |
| Contribución individual | Nota preparada | Identidad y evidencia personal reales. |

## Preguntas de Control

### 1. ¿Qué diferencia existe entre autenticación y autorización?

Autenticación determina la identidad mediante credencial/sesión; autorización evalúa capacidad y ámbito sobre una operación/recurso. SessionService valida token; CareerAdministrationService exige CAREER_ADMIN o lectura visible. Una sesión no concede todas las capacidades.

### 2. ¿Por qué un usuario autenticado no debería poder acceder automáticamente a todos los recursos?

Porque carreras/procesos están sujetos a ámbito y separación de responsabilidades. Un lector de A no debe recibir detalle, lista ni totales de B. UUID y enlace visible no sustituyen la comprobación del backend.

### 3. ¿Qué diferencia existe entre validación sintáctica y validación de reglas de negocio?

Sintaxis comprueba formato/tipo/longitud, por ejemplo UUID o fecha. Negocio verifica unidad activa, período permitido, duplicado normalizado y transición. Una fecha ISO válida puede exceder la vigencia de la carrera.

### 4. ¿Qué riesgos existen si se almacenan secretos directamente en el repositorio?

Pueden difundirse en clones, CI e historial aunque se borre el archivo actual; permitirían uso indebido de cuentas/datos. Usar configuración externa, mínimo privilegio y rotar/revocar ante exposición confirmada. No confundir contraseña sintética de fixture con secreto real sin clasificarla.

### 5. ¿Cómo puede una prueba demostrar que un control de autorización funciona correctamente?

Comparar mismo recurso/operación con principal autorizado y lector ajeno de fixture: permitir el primero y rechazar/ocultar el segundo sin efecto ni datos. Comprobar DB y totales si aplica. Un 401 anónimo no prueba permiso por objeto ni un mock cubre todo el filtro.

### 6. ¿Qué información no debería exponerse en una respuesta de error?

Contraseña, token completo, claves, SQL sensible, stack, detalles de infraestructura o datos ajenos. Devolver código público, detalle seguro y correlación; registrar diagnóstico protegido. La revisión actual no prueba todos los logs/500.

### 7. ¿Cómo se relacionan una amenaza, un control de seguridad y una prueba verificable?

T-02 acceso ajeno→permiso por capacidad/scope→VT-04 con lector A/carrera B y rechazo sin dato/efecto. Si falla, registrar SEC/CF, cambiar el punto de control y repetir regresión; conservar resultados y commit reales, no solo un tick documental.

## Conclusiones

Hay mecanismos Spring fuente y pruebas previas que permiten diseñar una verificación concreta, no una lista OWASP genérica. Esta entrega no acredita una nueva implementación DRF, una campaña APE4 ejecutada o un parche de seguridad. La práctica permanece parcial en esos puntos y en su cierre docente.

## Recomendaciones

Resolver tecnología y contrato; establecer sandbox; preparar fixture PostgreSQL; ejecutar VT con límites y clasificar fallos; aplicar la corrección mínima en el último punto confiable; reejecutar y versionar aportes reales. Mantener Actions manual y no usar despliegue ajeno para estas pruebas.

## Bibliografía / Referencias

[Fuentes compartidas](../soporte-semanas-4-5/FUENTES.md), F-01–12, OWASP O-01–08, HTTP H-01 y pytest D-05. La guía académica origina los resultados esperados; los mecanismos fuente no equivalen a seguridad certificada.

## Anexos

[PLAN_PRUEBAS.md](PLAN_PRUEBAS.md), prueba pytest propuesta, matrices compartidas, escaneo de metadatos, registro CF/CRUZADA, nota individual y manifiesto. Los JSON APE3 originales se referencian, no se recopian con fecha de Semana4.

## Declaración de uso de Inteligencia Artificial

ChatGPT/Codex apoyó revisión de fuentes, consulta oficial, elaboración de pruebas/documentos y comprobaciones estáticas de artefactos. No se ejecutó código del target ni se registraron feedback, commits, pruebas satisfactorias o cambios funcionales inexistentes. La revisión personal de los integrantes sigue pendiente.
