# ADR-0017: Cobertura documental, gobierno del proceso y cierres

**Fecha**: 2026-10-21.  
**Estado**: propuesto para implementación; reglas funcionales incorporadas a la especificación 005.

## Contexto

Relacionar evidencias solo con indicadores no determina qué fuente requerida está cubierta. Una jerarquía rígida tampoco representa todas las ediciones suministradas. El ciclo completo necesita registrar autorizaciones, informes, mejora y archivo sin que el seguimiento documental dependa de una valoración académica.

## Decisión

1. `RequiredSource` pertenece al indicador y versión; `SourceCoverage` relaciona una versión exacta de evidencia con esa fuente. Las coberturas declaradas no guardan aceptación duplicada: esta se deriva de decisiones documentales por cobertura.
2. Se cuentan fuentes distintas aplicables. Archivos duplicados no incrementan avance. Cada evidencia tiene una candidata remitida vigente; una nueva remisión no hereda aceptación. Un borrador sucesor no invalida su predecesora remitida.
3. `ModelNode` admite profundidad variable; el estándar es una definición académica por indicador y no un padre obligatorio.
4. Publicación, aplicabilidad normativa y autorización del proceso son decisiones separadas. El backend comprueba la política y conserva fundamento, autoridad y constancia.
5. Corte de seguimiento, cierre documental y cierre integral son operaciones diferentes. El primero no cambia el estado; el segundo no requiere P12–P15; el tercero usa una política integral versionada.
6. Informes emitidos y cierres son inmutables. Observaciones/aprobaciones y avances de mejora se anexan; no reescriben el contenido emitido ni las instantáneas anteriores.
7. Las máquinas de estados y ejemplos normativos de [domain-rules.md](../requirements/domain-rules.md) completan el diseño. La matriz histórica del ADR-0006 se amplía por este ADR, sin migrar silenciosamente autorizaciones históricas.

## Consecuencias

- El tablero puede reproducirse con política, fecha de corte y referencias exactas.
- La revisión requiere resultado por cobertura además de un estado global derivado.
- La migración debe mantener IDs, registrar clasificación pendiente de evidencia histórica y no inferir decisiones institucionales.
- La integración demanda pruebas de concurrencia, compatibilidad de contrato y cálculo con sucesoras; lint por sí solo no acepta una entrega.

## Alternativas consideradas

- Contar archivos: duplica cobertura y no identifica requerimientos faltantes.
- Considerar cualquier evidencia como suficiente para todas las fuentes: produce avances incorrectos.
- Cierre único con valoración obligatoria: bloquea el MVP documental.
- Modificar informes finales in situ: impide reconstruir decisiones y aprobaciones.

## Trazabilidad

FR-044–FR-068; AC-X04–AC-X15; tareas 005 T021–T102 y T113–T126. La vigencia y competencias institucionales continúan pendientes de validación según CL-011–CL-017.

## Aplicación parcial D6 (2026-10-24)

T058/T062 implementan el cálculo puro con política `SOURCE_COVERAGE_V1`:
numeradores y denominador enteros, porcentajes derivados con dos decimales
`HALF_UP`, cardinalidad por evidencias distintas y fuentes opcionales fuera
del cálculo. Un catálogo sin fuentes obligatorias es `NOT_CONFIGURED`;
un catálogo cuyas fuentes obligatorias están todas excluidas es
`NOT_APPLICABLE`. Ambos conservan porcentajes nulos.

La candidata es la última versión remitida de la evidencia completa,
no la última versión que contuvo cada fuente. Una sucesora remitida que
retira una cobertura no puede recuperarla desde una predecesora.
La autorización y las decisiones de aplicabilidad se resolverán antes
de invocar el calculador en T063. La persistencia de cortes y el cierre
documental siguen pendientes. Evidencia y límites en
[bases de D6](../../tests/acceptance/results/d6-progress-foundations.md).

T059/T063/T065 conectan el calculador a puertos públicos de evidencia y revisión,
sin repositorios cruzados. `PROGRESS_READ` se revalida por indicador antes de
contar y permite descubrir el proceso con sus indicadores visibles, sin otorgar
facultades de modificación. Los adaptadores leen candidatas remitidas completas,
rectificaciones vigentes y aplicabilidad por revisión. El tablero separa los
avances documentales de cualquier valoración académica. Las reglas ArchUnit
verifican esta independencia. Resultados en [consulta D6](../../tests/acceptance/results/d6-progress-query.md).

Los cortes actuales se guardan en transacciones serializables mediante T061 y
avance parcial T064/T066/T120. Un puerto público de proceso reserva la siguiente
revisión sin cambiar estado; un fallo de persistencia/auditoría revierte todo.
La lectura de históricos exige ámbito completo del proceso antes de cargar
contenido para evitar filtrar totales previos con permisos parciales actuales.
El JSON canónico preserva las referencias y se verifica con SHA-256; la base
impide sobrescritura/borrado. La reserva concurrente se verificó en PostgreSQL.
La transición de cierre continúa bloqueada: requiere coordinar remisiones y
revisiones mediante un bloqueo común y verificar integridad de archivos.

## Cierre documental implementado (2026-10-24)

La continuación resuelve esos requisitos con `DocumentaryProcessPort.lock` y
puertos explícitos de lectura/escritura. Cierre, evidencia, coberturas,
aplicabilidad y revisión bloquean la misma fila de proceso dentro de la
transacción y revalidan `ACTIVE`. PostgreSQL usa `FOR UPDATE`; SQLite serializa
el escritor mediante una actualización sin cambio de revisión. La autorización
se resuelve antes de exponer el estado a quien no puede operar.

La única política disponible es `DOCUMENTARY_STRICT_V1`: exige aceptación de
las fuentes obligatorias aplicables según cardinalidad y verifica disponibilidad,
tamaño y SHA-256 de los archivos referenciados. No exige valoración académica.
El corte y cierre canónicos, su auditoría y transición se confirman juntos;
los errores revierten todo. El controlador registra el rechazo después del
rollback para evitar otra transacción sobre la única conexión SQLite.
Los históricos son inmutables y verifican sus huellas al leer.
Evidencia de cierre, integridad y carreras de escritores:
[MVP D6](../../tests/acceptance/results/d6-documentary-mvp.md).

## Primer incremento académico manual (2026-10-24)

La valoración cualitativa consume exclusivamente puertos públicos de modelo,
proceso, cobertura y revisión. Requiere `INDICATOR_EVALUATE` en el indicador,
una regla manual `VALIDATED`, su escala exacta del modelo publicado y versiones
remitidas actuales con decisiones aceptadas sobre fuentes aplicables de ese
indicador. El método, etiqueta, limitaciones, justificación y referencias a
decisiones se conservan en el juicio; aceptar documentación no genera el juicio.

Cada evaluador mantiene su propia cadena de predecesoras. If-Match compara esa
revisión, sin sustituir juicios de otras personas ni consolidarlos. El bloqueo
común del proceso serializa los comandos con cambios documentales y cierres.
Se permiten valoraciones en ACTIVE, DOCUMENTARY_CLOSED y REOPENED; se rechazan
en preparación, suspensión y cierre integral. La valoración no cambia el estado
del proceso. El historial es append-only y la auditoría confirma o revierte con
el juicio. Cálculo, variables verificadas, discrepancias y consolidación siguen
pendientes; no se exponen éxitos provisionales para esas operaciones.

## Variables verificadas y cálculo versionado (2026-10-24)

La continuación incorpora `ratio-percent/1` al registro del backend. Ejecuta
solo `A / B * 100`, con A no negativo, B positivo, unidades iguales, tipos
INTEGER/DECIMAL y política de ausencia REJECT. El motor no interpreta fórmulas
arbitrarias ni confía en una cadena enviada por el cliente. Requiere además
regla publicada VALIDATED y valida todo el contrato al usarla.

Captura, verificación y ejecución son registros distintos e inmutables. Cada
variable conserva sus sustentos aceptados exactos y período dentro del proceso;
corregir crea otra revisión propia. La verificación registra persona, fecha y
fundamento explícitos; la función evaluadora autorizada puede verificar el
conjunto, sin inferir una separación de personas no establecida por el dominio.
Al verificar y calcular se revalidan los sustentos y la revisión vigente.

Una ejecución por conjunto verificado conserva entradas, regla, escala, umbral,
aritmética DECIMAL128, resultado, responsables, justificación y limitaciones.
Los decimales de entradas/resultados viajan como texto exacto al cliente. El
umbral se aplica al resultado DECIMAL128 conservado, sin redondeo de pantalla.
Las revisiones nuevas no alteran cálculos previos. Los comandos comparten el
bloqueo del proceso y auditoría transaccional; los rechazos se auditan después
del rollback. Conciliación y consolidación final continúan pendientes.

## Resolución explícita de discrepancias (2026-10-25)

V023 incorpora procedimientos versionados por proceso/indicador, discrepancias
y resoluciones inmutables. Los evaluadores autorizados pueden declarar un
desacuerdo con los juicios actuales de dos personas distintas; se preservan
sus fundamentos completos. Configurar el procedimiento o resolver exige además
`ACADEMIC_RECONCILE` y una asignación vigente validada por
`InstitutionalAuthorityPort`, con fundamento y constancia. La nueva capacidad
no se concede automáticamente a coordinadores ni evaluadores del catálogo.

El método soportado `SELECT_EXISTING_JUDGEMENT_V1` selecciona un juicio existente
con fundamento y participantes explícitos: quienes emitieron los juicios y
quien resuelve. No calcula promedios ni genera una valoración arbitraria.
La resolución conserva la versión exacta del procedimiento, asignación, actor,
fecha y selección. Una resolución por discrepancia y el bloqueo del proceso
evitan sobrescrituras; la corrección usa otra discrepancia y predecesora.

El estado de consolidación es una proyección. Juicios nuevos, procedimiento
nuevo, pérdida de aceptación o variables posteriores hacen que una resolución
previa deje de ser aplicable. Se muestran pendientes y marcas de revisión sin
editar los payloads originales. La lectura exige ámbito evaluador y pagina
historiales de juicios/discrepancias por separado. Auditoría de éxito y escritura
confirman juntas; el rechazo se registra después del rollback. La competencia
y el procedimiento sintéticos prueban el software, no su vigencia institucional.
