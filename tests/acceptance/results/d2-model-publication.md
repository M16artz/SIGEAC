# D2 — Publicación por indicador y cinco configuraciones sintéticas

Fecha: 2026-10-21. Continuación de [las bases D2](d2-model-foundations.md).
Este bloque valida el motor de aplicación; no constituye aceptación de D2 ni
verificación de normativa institucional.

## Análisis de especificación, plan y tareas

Se contrastaron US2.1–US2.4, FR-042/FR-044–FR-047, AC-X05/AC-X10, el modelo de
datos, RuleInput/VariableDefinition/Threshold del contrato objetivo y
005/T006/T022–T026. Se conservan dos puertas: T114/T116/T027 para procedencia y
aplicabilidad, y T024/T028/T029 para migración relacional y recorrido web v2.
No se detectó contradicción crítica para implementar los comandos internos.

## Comportamiento implementado

- `ModelConfiguration` conserva definiciones, fuentes, escalas y reglas por
  indicador en la versión exacta; no exige escala ni fórmula global a un modelo
  configurado. Los snapshots anteriores sin `configuration` siguen leyéndose.
- El servicio existente crea, modifica y publica configuraciones mediante el
  mismo flujo transaccional de permisos, revisión optimista, aprobador distinto
  del autor y auditoría. Publicado/sustituido no admite edición directa ni una
  segunda publicación. La sucesora conserva configuración e identidades.
- Un conflicto que aparece durante el `UPDATE` del repositorio, después de la
  comprobación inicial, conserva `412 STALE_MODEL_VERSION`; no se transforma en
  error genérico ni en éxito auditado. SQLite verifica que la escritura obsoleta
  no cambia el snapshot confirmado.
- La publicación verifica definiciones para todos los indicadores, fuentes,
  pertenencia, referencias de escala/regla, naturaleza, identidades únicas,
  opciones de escala y umbrales que no se solapen ni apunten a opciones ajenas.
- Las variables exigen código, tipo, unidad y definición de validación. Los
  límites numéricos abiertos, cerrados e indefinidos se distinguen expresamente.
- Una regla cuantitativa `DRAFT` puede conservar metodología incompleta con
  automatización deshabilitada. Una `VALIDATED` exige fórmula documental,
  variables, umbrales cuando utiliza escala y coincidencia exacta de clave y
  versión de estrategia registrada/validada por el backend. `RETIRED` no se publica.
- `ValidatedStrategyPort` está vacío por defecto. El código no interpreta ni
  ejecuta fórmulas y no implementa algoritmos I7/P12. La prueba positiva utiliza
  una estrategia sintética proporcionada por el test, no una validación oficial.
- El catálogo devuelve la configuración completa, y la comparación detecta
  cambios en reglas, fuentes y escalas aunque el árbol no cambie. V1 conserva su
  transporte y rechaza toda configuración nueva antes de perder información.

## Cinco fixtures

`tests/fixtures/models/five-model-configurations.json` contiene General,
Medicina, Enfermería, Odontología y Derecho, con profundidades de 2 a 5 niveles,
indicadores directos/anidados, dos naturalezas, cuatro modalidades de fuente,
fuentes opcionales, cardinalidad variable, unidades y umbrales propios.

`normative-manifest.json` enlaza esas cinco configuraciones y declara
`synthetic=true`, `institutionalValidity=false`, `PENDING_VALIDATION` y páginas/
huellas sin corroborar. No sustituye el manifiesto de verificación T114, no
afirma cantidades oficiales de indicadores ni carga datos en el arranque.

## Evidencia automatizada

| Prueba | Alcance |
|---|---|
| `CompleteModelPublicationTest` | Fuentes/definiciones ausentes o ajenas; escalas/reglas; duplicados; publicación sin automatización; estrategia exacta; metadatos incompletos; umbrales; permisos; revisión; inmutabilidad y sucesión. |
| `FiveModelConfigurationTest` | Cinco fixtures por el mismo servicio/catálogo; profundidades/modalidades distintas; round-trip JSON; manifiesto pendiente y automatización deshabilitada. |
| `ValuationRuleTest` | Variables/unidades; umbrales abiertos/cerrados/ilimitados; separación de escala y prohibición de cálculo manual/no validado. |
| `FlexibleModelPersistenceTest` | Configuración completa en SQLite; JSON legado sin los campos nuevos; contenido anterior intacto. |
| `ModelComparisonServiceTest` | Cambios de metodología y escala con árbol idéntico; conservación del predecesor. |

La regresión final con Eclipse Temurin 25.0.4.1+1, Maven 3.9.16 y perfil SQLite
terminó con **153 casos: 145 aprobados, 0 fallos, 0 errores y 8 omitidos**.
Incluye los 11 casos de `CompleteModelPublicationTest`, 2 de los cinco fixtures,
3 de reglas, 3 de comparación y el arranque Spring/ArchUnit completo. Los ocho
omitidos son pruebas PostgreSQL condicionadas a `SEC_RUN_DATABASE_TESTS`, no
habilitada para este bloque. No se omite el enforcer ni se cambia el toolchain.

Redocly 2.65.0 validó los contratos común, 004 y 005. El verificador informó
70 operaciones comunes, 77 objetivo, 12 compartidas y cero incompatibilidades.
El verificador de datos sintéticos aprobó; no encontró los patrones prohibidos
que inspecciona. No es una revisión exhaustiva de secretos. `git diff --check`
y la comprobación del índice también aprobaron.

## Reproducción y límites

Desde `backend/`, con Java 25:

```powershell
.\mvnw.cmd test
```

En este entorno se reutilizaron JDK/Maven y caché de dependencias de `target`,
con `-Dmaven.repo.local=target/maven-repository` y `-o` (sin red). Informes:
`backend/target/surefire-reports`; log: `backend/target/d2-full-tests.log`.

Se completan 005/T006, T022, T023, T025 y T026 con la regresión aprobada. No se
crea/aplica V009 ni se habilitan endpoints v2. Continúan T024/T027–T030/T114/T116,
la verificación de fuentes originales, PostgreSQL y el recorrido de navegador.
Antes de exponer modelos configurados a procesos, la aplicabilidad pendiente
debe bloquear la activación conforme T116. Los fixtures no acreditan vigencia.
