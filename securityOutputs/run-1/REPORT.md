# Auditoría de seguridad de SIGEAC

**Estado: INCOMPLETA; revisión estática parcial.** Fecha: 28 de octubre de 2026. Perfil solicitado por defecto: standard; no se fijó presupuesto de agentes. Se revisó el árbol de trabajo actual, con cambios locales, sobre commit base `61988addaf2611cb239574a528b3e04d486bd5e6`. Los binarios de `backend/target` no se trataron como otra revisión del fuente.

Alcance: backend Spring, frontend Angular, configuración, CI y scripts seleccionados. Mobile está vacío. Política: fuente y pruebas locales aisladas solamente; **no se ejecutó código, builds ni pruebas del proyecto y no se enviaron solicitudes a servicios desplegados**. Se consultaron páginas públicas oficiales de avisos, sin interactuar con instancias del producto. No se modificó el código ni se sobrescribieron cambios locales.

La limitación de cierre es técnica y está registrada en run-metadata.json: ambos CLI obligatorios de la habilidad fallan en Windows con `OS no-follow and nonblocking input protection is unavailable`. Las funciones exportadas de validación de esquema/semántica sí pasaron en memoria con datos generados por el auditor; esto no acredita que los CLI hayan pasado. Además, no se estableció el sandbox OS completo requerido para reproducir defectos. No se desactivó ninguna protección del validador.

No había cobertura previa compatible en la carpeta externa de auditoría. El documento existente `docs/technical/actores-accesos-riesgos.md` contiene hipótesis estáticas anteriores; se usó como contexto y se volvieron a leer las fuentes, sin arrastrar confirmaciones ni tratarlo como prueba de ejecución.

## Resultado

Se conservaron **7 candidatos needs_validation, 0 vulnerabilidades confirmadas mediante reproducción**. Cada candidato pasó revisión independiente de fuente y una segunda verificación independiente del registro final. Cero confirmaciones no significa ausencia de vulnerabilidades. Los controles de sesión y ámbito existen, pero varias rutas no aplican de forma uniforme el control del recurso o de la transición.

No se asigna gravedad a candidatos pendientes. El orden siguiente prioriza la validación por autoridad y recursos afectados.

| Orden | Candidato | Frontera y posible resultado | Punto principal |
|---|---|---|---|
| NV-01 | El alta de asignaciones no comprueba la autoridad sobre el ámbito destino | Administrador de A concede funciones o excepciones en B. | `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/IdentityAdministrationService.java:157` |
| NV-02 | La consulta individual de revisión documental omite la autorización del recurso | Sesión con UUID conocido lee resultados y comentarios de otra carrera. | `backend/src/main/java/ec/edu/unl/sec/documentreview/application/DocumentReviewService.java:117` |
| NV-03 | La consulta de auditoría pierde las restricciones de proceso del auditor | Auditor limitado a P1 obtiene eventos/conteos de P2 de la misma carrera. | `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/IdentityAdministrationService.java:347` |
| NV-04 | El alta de asignaciones crea excepciones sin exigir el permiso específico | Otorgante con permiso de asignar crea excepción sin permiso de excepciones. | `backend/src/main/java/ec/edu/unl/sec/identityaccess/application/IdentityAdministrationService.java:157` |
| NV-05 | La autorrevisión acepta una excepción declarada por el propio revisor | Remitente histórico ahora revisor declara su propia excepción y acepta evidencia. | `backend/src/main/java/ec/edu/unl/sec/documentreview/application/DocumentReviewService.java:71` |
| NV-06 | Las mutaciones de cobertura y remisión omiten el control de autor del borrador | Responsable B modifica/remite borrador de A y la remisión se atribuye a A. | `backend/src/main/java/ec/edu/unl/sec/evidence/application/SubmitEvidenceService.java:48` |
| NV-07 | El registro de avances de mejora omite la suspensión del proceso | Usuario con permiso ordinario registra avances durante SUSPENDED. | `backend/src/main/java/ec/edu/unl/sec/improvement/application/ActionProgressService.java:68` |

Para cada candidato, [NEEDS-VALIDATION.md](NEEDS-VALIDATION.md) conserva la ruta completa, controles presentes, condiciones, bloqueo y fixture mínimo. No contiene instrucciones de pruebas sobre producción. [findings.json](findings.json) es el registro estructurado.

## Controles que deben verificarse y reforzarse al resolver los candidatos

- **NV-01:** En assign y createException, comprobar isAllowed(actor, operación, ámbito destino) antes de insertar. Mantener separada cualquier autoridad global explícita.
- **NV-02:** Resolver la versión a su evidencia/proceso y exigir la política de lectura correspondiente antes de consultar o devolver la revisión; una sesión no autoriza el objeto.
- **NV-03:** Conservar carrera/proceso/indicador y conflictos efectivos en selección y conteo de eventos. Aplicar la misma política a filas, total y eventos sin ámbito.
- **NV-04:** Exigir ACCESS_MANAGE_EXCEPTIONS y competencia sobre el ámbito para la excepción incorporada, igual que en su ruta independiente.
- **NV-05:** Resolver una autorización de excepción vigente y vinculada a versión/autor; no aceptar el booleano del propio solicitante como aprobación.
- **NV-06:** Usar requireEditableVersion para las mutaciones de borradores. Si existe delegación intencional, hacerla explícita y auditar al actor efectivo.
- **NV-07:** Aplicar el control de suspensión en el punto de escritura compartido por avances y decisiones, conservando solo las continuaciones expresamente autorizadas.

Estas son propuestas de corrección en el último punto de decisión confiable; no se aplicaron cambios. Cada fixture debe comprobar rechazo sin escritura o sin datos ajenos, preservando los casos legítimos.

## Controles positivos observados

- Tokens opacos de 32 bytes con SecureRandom; hashes de tokens/CSRF, BCrypt, comprobación por petición de identidad activa, expiración y revocación.
- Cookie HttpOnly y SameSite=Lax; perfil prod fuerza Secure. Hay filtro CSRF propio con comparación constante en mutaciones autenticadas: `csrf.disable` de Spring no equivale aquí a ausencia de protección.
- Autorización central comprueba función, ámbito, vigencia y conflictos; varios servicios preservan controles por proceso/indicador y autorización institucional.
- Archivos con límite por carga, inspección de contenido, UUID de almacenamiento, hash/tamaño, validaciones de rutas y descarga attachment con pertenencia a versión.
- Catálogo de modelos protege publicación, revisión, formato y separación autor/aprobador. Los catálogos globales no se trataron automáticamente como recursos privados de una carrera.

## Endurecimiento y dependencias

- Restringir el interceptor CSRF al origen/prefijo esperado antes de futuras llamadas HTTP externas. Actualmente ApiClient ancla las rutas a /api/v1; no se estableció exfiltración alcanzable.
- Fijar acciones de CI por digest y explicitar permisos mínimos. Los workflows revisados son manuales; una etiqueta mutable por sí sola no demuestra ejecución privilegiada por un atacante.
- Confirmar mediante observación del propietario perfil prod, TLS/proxy, ACL del almacenamiento y ubicación fuera de raíz pública. La fuente expresa intención, no el estado desplegado. .env está ignorado y no aparece rastreado; no se imprimieron sus valores.
- No se completó un inventario transitivo resuelto ni un escaneo SCA. No se ejecutó npm audit, Maven ni instalaciones. Versiones declaradas: Spring Boot 4.1.1, Angular 22.2.3, tika-core 3.2.3. El aviso [GHSA-jfgx-762w-99vr de Angular](https://github.com/angular/angular/security/advisories/GHSA-jfgx-762w-99vr) excluye explícitamente 22.x, por lo que no se adjudicó ese CVE al proyecto. Los avisos de Spring examinados requieren [DPoP](https://spring.io/security/cve-2026-41707/) o [WebAuthn con sesiones distribuidas](https://spring.io/security/cve-2026-47841/); no se encontró ese mecanismo en la autenticación revisada. Este contraste puntual no acredita que todas las dependencias estén libres de CVE.

## Cobertura y pendientes

El ledger contiene **24 unidades: 11 covered por lectura de invariantes seleccionadas, 4 candidate con los siete registros, 7 deferred y 2 out_of_scope**. “Covered” se limita a los archivos, clase e invariantes del ledger, sin implicar revisión exhaustiva de un módulo ni prueba dinámica.

Las brechas retenidas tras ambos críticos para otra pasada son:

- **quantitative:** Captura/verificación/cálculo cuantitativos necesitan revisión específica; lectura de evaluación manual no acredita esta superficie.
- **alternate-audit:** No se profundizó en permisos/filtros alternativos de auditoría de modelos/procesos; critic detectó posible mezcla de eventos sin ámbito.
- **file-recovery:** Lectura parcial no establece política de retención/recuperación ni efecto sobre capacidad compartida.
- **login-capacity:** Trabajo BCrypt previo a autenticación, cardinalidad y retención de intentos no verificados; no se afirma DoS.
- **backup-restore:** Herramientas de restauración de operador requieren revisión específica de límites de entorno/copia y estado restaurado.

- **browser:** falta una revisión específica de fuentes y sinks DOM y templates. Se archivó el intento previo; revisar HTTP/sesión no cierra la clase XSS.
- **aggregate-upload-capacity:** faltan límites agregados y contabilidad por principal; el límite por carga no demuestra contención del disco compartido.

El crítico final detectó estas dos brechas y una atribución de evidencia de archivos: se separaron las lecturas de root de las del hunter. Las correcciones están en el ledger; no se declara una pasada de cobertura limpia.

Fuera de alcance: implementación móvil inexistente y controles/datos/servicios de un despliegue real. No se demostró DoS, SSRF, XSS ni exposición de secretos a partir de simples desviaciones de buenas prácticas. Los controles visibles que refutaron esas hipótesis se documentan en la cobertura.

El cierre no declara cobertura limpia: las brechas permanecen explícitas, el sandbox impide reproducción y los CLI obligatorios no pudieron completarse. Para continuar, ejecutar los fixtures de NEEDS-VALIDATION.md en un entorno descartable que cumpla todos los límites de la habilidad y ejecutar ambos CLI sin retirar sus protecciones.
