# Actores, decisiones y reglas verificables

**Estado**: especificación de producto propuesta para implementación; pendiente de validación institucional donde se indica.  
**Fecha**: 2026-10-21.  
**Referencias**: [procesos P01–P16](process-flows.md), [historias y requisitos](../../specs/005-complete-evaluation-workflows/spec.md).

Las reglas de este documento son decisiones del software. Las referencias a competencias UNL son puntos que deben contrastarse con el documento institucional y las designaciones reales. Este documento no certifica vigencia normativa.

## Vocabulario común

| Concepto | Definición |
|---|---|
| Modelo | Identidad del catálogo; General, Medicina, Enfermería, Odontología o Derecho. |
| Versión de modelo | Edición con procedencia, jerarquía, estándares, fuentes y reglas inmutables al publicarse. |
| Publicación | Aprobación técnica/metodológica de una configuración; diferente de vigencia normativa. |
| Estándar | Texto y elementos de análisis asociados a un indicador; no exige ser un nivel entre criterio e indicador. |
| Fuente requerida | Requisito de información con identidad, período y condiciones de cobertura; admite archivos, enlaces o datos. |
| Evidencia | Identidad que agrupa versiones sucesivas de información aportada. |
| Cobertura | Relación explícita de una versión con una fuente requerida; declarar cobertura no significa aceptarla. |
| Revisión documental | Dictamen sobre una versión y sobre las fuentes que esa versión cubre. |
| Valoración académica | Juicio o cálculo por indicador y regla de la versión del modelo. |
| Corte documental | Instantánea de seguimiento que conserva pendientes; no cierra el proceso académico. |
| Cierre documental | Final de la recepción/revisión ordinaria según política; puede conservar pendientes autorizados. |
| Cierre integral | Decisión posterior que satisface o exceptúa prerrequisitos institucionales del proceso completo. |
| Archivo | Conservación íntegra y consulta autorizada de un cierre; no implica eliminación ni publicación abierta. |

## Matriz de responsabilidades y permisos

Toda función requiere asignación vigente y ámbito. Pertenecer a una comisión no concede todos los permisos del sistema. Un equipo tiene miembros individualizados y cada actuación registra a la persona que la ejecutó.

| Proceso | Ejecutor autorizado | Participantes/decisor | Restricción |
|---|---|---|---|
| P01 | Usuario; administrador de identidades | Administrador técnico dentro de sus capacidades | Privilegio técnico no otorga competencias documentales/académicas. |
| P02 | Administrador funcional | Responsable de unidad/carrera designado | No borrar carreras con historial; permisos limitados a su ámbito. |
| P03 | Administrador de modelos | Revisor metodológico y aprobador designados | Autor y aprobador distintos; vigencia no inferida. |
| P04 | Administrador de modelos | Coordinación de calidad valida metodología | Publicado implica solo lectura; cambios mediante sucesora. |
| P05 | Coordinador prepara | Dirección de Carrera y consulta al Decanato, sujetas a validación del instructivo | Activación exige constancia y función autorizadora vigente. |
| P06 | Coordinador o comisión con capacidad de planificar | Responsables y equipos designados | No autoasignar autoridad de aprobación; conflictos requieren excepción. |
| P07 | Responsable documental o comisión de recolección | Docentes/administrativos con asignación | Solo fuentes e indicadores dentro del proceso activo y su ámbito. |
| P08 | Responsable documental | Revisor recibe en bandeja | Reautorizar al enviar; verificar archivo y cobertura. |
| P09 | Revisor designado | Responsable recibe observaciones | Bloquear autorrevisión salvo excepción expresa y auditable. |
| P10 | Responsable documental | Revisor decide sobre nueva versión | Predecesora no editable; no se hereda aceptación automáticamente. |
| P11 | Coordinador, supervisor o consultor | Auditor dentro de ámbito | Conteos y exportaciones respetan los mismos filtros que el detalle. |
| P12 | Evaluador o miembro autorizado del comité | Comité resuelve discrepancias | Cuenta técnica no finaliza; fuentes y reglas identificables. |
| P13 | Comisión de evaluación | Comité y coordinación de calidad | Hallazgos fundados; pendientes visibles; agregación solo validada. |
| P14 | Comisión elabora | Instancias de revisión y autoridad aprobadora, Decanato sujeto a confirmación | Informe pendiente/devuelto no se presenta como aprobado. |
| P15 | Responsable de mejora | Comisión/autoridad aprueba; verificador comprueba | El ejecutor reporta avance; no presume verificación institucional. |
| P16 | Coordinador cierra; archivista conserva | Autoridad resuelve excepciones | Consulta por ámbito; rectificación explícita sin sobrescribir cierres. |

## Registro de decisiones institucionales

Los responsables siguientes son funciones propuestas para solicitar validación, no nombramientos ni fechas de aprobación. Cada resolución futura registra persona, fecha efectiva, documento y versión de política. Hasta entonces se usan datos sintéticos y la interfaz debe indicarlo.

| ID | Decisión | Responsable de validación propuesto | Momento límite | Supuesto reversible / efecto |
|---|---|---|---|---|
| CL-011 | Versiones documentales y vigencia por carrera | Coordinación de calidad y Dirección de Carrera | Antes de activar un proceso institucional | Cinco ediciones suministradas como referencias; aplicabilidad `PENDING_VALIDATION` impide uso institucional. |
| CL-012 | Autoevaluación interna o evaluación externa | Dirección de Carrera / coordinación del proyecto | Antes de ampliar alcance | Solo autoevaluación interna; resultados externos requieren especificación separada. |
| CL-013 | Registro manual y automatización por indicador | Revisor metodológico y comité evaluador | Antes de finalizar P12 para cada regla | Manual si la metodología lo permite; fórmula no validada queda pendiente. |
| CL-014 | Carga representativa o exhaustiva de indicadores | Docente / responsable académico del proyecto | Antes de aceptar D2 | MVP: cinco estructuras representativas; catálogo exhaustivo es trabajo de datos adicional. |
| CL-015 | Autorizar inicio, suspensión, reapertura y excepciones | Dirección de Carrera, Decanato e instancias designadas | Antes de aplicar política institucional en P05/P16 | Política sintética con autoridad y constancia obligatorias. |
| CL-016 | Informes: formato, socialización y aprobación | Comisión y autoridad competente | Antes de emitir un informe institucional en P14 | Plantilla sintética rotulada como tal; se registran decisiones sin atribuir validez oficial. |
| CL-017 | Verificación de acciones y requisitos de cierre integral | Comisión, responsables de mejora y archivo | Antes de aprobar planes/cierre institucional | Política versionada; no exigir terminar todas las acciones si continúan tras la evaluación salvo regla explícita. |

CL-001–CL-010 se conservan en la [especificación general](spec.md#decisiones-institucionales-abiertas). No se reemplazan por estos puntos: ambas series deben resolverse con procedencia y fecha.

## Procedencia de las cinco configuraciones

| Configuración | Referencia aportada por el usuario | Estado de corroboración documental |
|---|---|---|
| General 2024 | Modelo genérico para evaluación del entorno de aprendizaje de carreras de grado, marzo 2024 | Pendiente contrastar PDF original, páginas, códigos, 5 criterios, 8 subcriterios y 31 indicadores. No confundir con General 2015. |
| Medicina 2018 | Modelo específico Medicina, actualización 2018 | Pendiente cotejar edición, matriz e indicadores con el original. |
| Enfermería 2016 | Modelo específico de carrera de Enfermería, octubre 2016 | Pendiente cotejar versión y particularidades de prácticas/simulación. |
| Odontología 2018 | Modelo específico Odontología actualizado | Pendiente confirmar edición y fuentes particulares de unidades asistenciales. |
| Derecho matricial | Modelo específico Derecho actualizado | Fecha/edición pendientes de identificación en la matriz original. |
| Procedimiento UNL | Resolución Nro. 037-R-UNL-2022, instructivo de aseguramiento de calidad | Pendiente contraste artículo por artículo y registro de atribuciones, especialmente inicio e informes. |

Cada `NormativeReference` debe registrar título, organismo, edición, fecha conocida, localización, huella del archivo si se conserva, páginas/disposición citadas, usuario que verificó y fecha de verificación. Un indicador registra sus referencias, no solo la referencia global del modelo. No se afirma vigencia por haber cargado el archivo.

## Estados y transiciones

### Modelo y aplicabilidad

- Ciclo técnico: `DRAFT → PUBLISHED → SUPERSEDED`. El contenido publicado se conserva; sustitución es un evento/metadato de ciclo, no una edición de sus estándares.
- Aplicabilidad por carrera/versión: `PENDING_VALIDATION`, `APPLICABLE`, `NOT_APPLICABLE`, `EXPIRED`. Decisión y vigencia independientes del ciclo técnico.
- En piloto sintético puede utilizarse una configuración rotulada como demostración; nunca activa un proceso identificado como institucional.

### Proceso

| Desde | Acción | Hasta | Condición |
|---|---|---|---|
| `PREPARATION` | Activar | `ACTIVE` | Carrera activa, modelo publicado/aplicable y autorización verificada. |
| `ACTIVE` | Suspender | `SUSPENDED` | Actor autorizado, motivo y constancia. |
| `SUSPENDED` | Reanudar | `ACTIVE` | Decisión autorizada; no modifica modelo ni amplía plazos por sí sola. |
| `ACTIVE` | Finalizar fase documental | `DOCUMENTARY_CLOSED` | Política documental y excepciones satisfechas; no depende de P12–P15. |
| `DOCUMENTARY_CLOSED` | Iniciar cierre integral | `CLOSING` | P12–P14 y requisitos de la política integral verificados. |
| `CLOSING` | Confirmar cierre integral | `CLOSED` | Revalidación transaccional y snapshot íntegro. |
| `CLOSING` | Devolver a análisis | `DOCUMENTARY_CLOSED` | Motivo y decisión de devolución; conserva intentos de cierre. |
| `DOCUMENTARY_CLOSED` o `CLOSED` | Reabrir | `REOPENED` | Autorización, motivo y tipo de rectificación. |
| `REOPENED` | Reanudar | `ACTIVE` | Si requiere corregir recepción/revisión; fija el modelo original. |
| `REOPENED` | Reanudar análisis | `DOCUMENTARY_CLOSED` | Si solo rectifica evaluación/informe; documentación permanece congelada. |

Un corte de seguimiento se crea en `ACTIVE` sin cambiar estado. Tras `DOCUMENTARY_CLOSED` se permiten P12–P15 conforme al ámbito y política, pero P07–P10 requieren reapertura. Los avances de mejora pueden continuar después de `CLOSED` si la política los permite como registros nuevos, sin alterar el snapshot de cierre. `DRAFT` histórico se mapea a `PREPARATION` con migración; activar datos históricos no inventa una autorización retroactiva.

### Evidencia y revisión

`EvidenceVersion.status` solo tiene `DRAFT|SUBMITTED`. `EN_REVISION` es la proyección de una versión remitida sin decisión final vigente; `ACEPTADA`, `OBSERVADA` y `RECHAZADA` se derivan de `DocumentReviewDecision`. No se almacena un segundo estado documental en la evidencia.

`SUBMITTED` permanece inmutable. Una corrección crea otro `DRAFT` con `predecessorId`. La revisión es append-only: rectificar requiere capacidad adicional, motivo y referencia a la decisión anterior; dos solicitudes concurrentes no pueden dejar dos decisiones vigentes.

Una versión puede cubrir varias fuentes. Cada decisión incluye el resultado por `SourceCoverage`. El resultado global es `ACCEPTED` si todas se aceptan, `REJECTED` si todas se rechazan y `OBSERVED` en una combinación o cuando queda alguna observada. Así puede aceptarse una fuente y observarse otra sin aprobar ambas por inferencia.

### Informes y mejora

- Documento de informe: borrador editable; al emitir `PRELIMINARY` o `FINAL` se crea snapshot inmutable. Socialización, observaciones y aprobación son eventos anexos; nunca cambian los bytes de una versión emitida.
- Proyección de tramitación: `DRAFT → PRELIMINARY → UNDER_OBSERVATION → FINAL → APPROVED|RETURNED`. Una corrección crea otra versión y su tramitación; la aprobación identifica una versión final exacta.
- Acción: `PROPOSED → APPROVED → IN_PROGRESS → PENDING_VERIFICATION → VERIFIED`. Devolución vuelve a propuesta con observación; verificación insuficiente vuelve a ejecución. `OVERDUE` es una marca derivada de fecha límite y ausencia de verificación satisfactoria, no reemplaza el estado.
- Discrepancia académica: valoración permanece `PENDING_RECONCILIATION`; la resolución registra participantes, decisión y método de conciliación; la evaluación previa continúa consultable.

## Cálculo documental sin ambigüedad

Sea S el conjunto de **IDs distintos** de fuentes obligatorias de indicadores aplicables, de la versión fijada al proceso, sin exclusión vigente autorizada. Las fuentes opcionales se muestran pero no entran al denominador.

Sea R el subconjunto de S cubierto al menos por una versión remitida vigente. Los borradores no cuentan. Sea A el subconjunto de S que satisface la cardinalidad de cobertura de la fuente mediante decisiones vigentes `ACCEPTED`.

`recopilación = |R| / |S| × 100`; `revisión documental = |A| / |S| × 100`.

Reglas de reproducción:

- Una fuente se cuenta una vez aunque tenga varios archivos, evidencias o enlaces. Cardinalidad mínima > 1 exige esa cantidad de evidencias distintas aceptadas; el mínimo de archivos dentro de una evidencia se valida por separado. Por defecto `cardinalityMin=1`.
- Un borrador sucesor no invalida una versión remitida previa. Al remitir la sucesora pasa a ser la candidata vigente para **esa evidencia**; su decisión no se hereda. Otras evidencias independientes pueden seguir cubriendo la misma fuente.
- El historial sigue mostrando aceptaciones anteriores, pero el avance actual usa candidatas vigentes. Una rectificación de decisión recalcula; un snapshot cerrado conserva el corte original.
- Retirar una cobertura en una sucesora no borra su relación histórica y puede reducir el avance actual.
- Si no hay fuentes configuradas: porcentajes `null`, estado `NOT_CONFIGURED`. Si todas las fuentes/indicadores se excluyeron con decisiones válidas: porcentajes `null`, estado `NOT_APPLICABLE`. Ninguno equivale a 100 %.
- Si existen fuentes y ninguna evidencia: ambos porcentajes son 0.
- Redondear a dos decimales solo para presentar; guardar numeradores, denominador, política, fecha de corte y referencias exactas.
- El filtro de ámbito se aplica antes de contar; la respuesta informa el alcance consultado. No revelar totales de ámbitos ajenos.
- Avance de valoración significa indicadores con valoración final vigente / indicadores académicos aplicables. No expresa el grado de cumplimiento del estándar y no se mezcla con los dos avances documentales.

## Reglas de cierre

El corte documental conserva el estado observado, incluso con pendientes. El cierre de fase documental aplica una política explícita: por defecto exige que toda fuente obligatoria tenga revisión concluida y ningún archivo referenciado esté perdido/corrupto. Una fuente rechazada puede cerrar como pendiente identificado solo si la política o una excepción autorizada lo permite. El cierre documental no exige cumplimiento académico.

El cierre integral exige los requisitos de su política (por defecto informe final aprobado, resultados obligatorios finales o limitaciones autorizadas, plan de mejora aprobado cuando haya debilidades y consistencia de archivos). Una acción a futuro no bloquea automáticamente el cierre: su seguimiento conserva un historial independiente. Una excepción no elimina pendientes; los registra en el cierre.

## Criterios transversales de aceptación

| ID | Dado / Cuando | Entonces |
|---|---|---|
| AC-X01 | Usuario fuera de ámbito intenta leer, contar, descargar o modificar un objeto | Respuesta segura, sin datos ni cambio parcial y con auditoría exigida. |
| AC-X02 | Cuenta desactivada o sesión vencida intenta actuar | Se deniega aunque el frontend conserve datos anteriores. |
| AC-X03 | Dos actores envían el mismo `If-Match` al modificar | Solo una operación confirma; la otra recibe `412` y no pierde cambios silenciosamente. |
| AC-X04 | Falta autorización de inicio o modelo no aplicable en proceso institucional | Se conserva `PREPARATION`; no se crea activación parcial. |
| AC-X05 | Indicador directo a criterio y otra estructura con varios niveles | Ambas se publican si cumplen sus reglas de versión; un ciclo se rechaza. |
| AC-X06 | Una versión cubre dos fuentes; el revisor acepta una y observa otra | Solo la primera puede entrar en A; resultado global observado y ninguna valoración académica creada. |
| AC-X07 | v1 aceptada tiene borrador v2; luego v2 se remite sin decisión | v1 cuenta hasta la remisión; después esa evidencia no suma aceptación actual hasta revisar v2. |
| AC-X08 | S contiene cuatro fuentes, R tres y A dos | Se muestra 75 % y 50 %, incluso si varias evidencias repiten una fuente. |
| AC-X09 | S vacío por configuración ausente o por exclusiones autorizadas | `null` con causas distintas; no dividir entre cero ni mostrar 100 %. |
| AC-X10 | Falta variable, fórmula no soportada o regla no validada | No ejecutar cálculo, registrar pendiente y conservar definición documental. |
| AC-X11 | Dos evaluadores discrepan | Registrar ambos fundamentos, dejar pendiente conciliación y conservar resolución posterior. |
| AC-X12 | Informe final devuelto o aprobación pendiente | No identificarlo como aprobado; corrección emite versión sucesora. |
| AC-X13 | Acción cambia responsable, meta o plazo | Nueva revisión auditada; vencimiento previo e informes históricos permanecen. |
| AC-X14 | Cierre con bloqueos o falla de conservación | No confirmar cierre; mostrar bloqueos sin resultados ficticios. |
| AC-X15 | Proceso archivado requiere rectificación | Autorizar reapertura/rectificación, conservar snapshot y crear sucesora de los objetos afectados. |
| AC-X16 | Fuente contiene datos estructurados o referencia institucional | Conservar valor/referencia, unidad y período si aplican; no exigir PDF ni afirmar disponibilidad de enlace remoto. |

