# Lista de calidad de gobierno y seguridad: Sistema de Evaluación de Carreras

**Propósito**: Revisar que los requisitos expresen de forma completa, clara, consistente y medible las separaciones del dominio, seguridad por ámbito, versionado, trazabilidad y límites del MVP.
**Creada**: 2026-10-18
**Funcionalidad**: [spec.md](../spec.md)

**Nota**: Esta lista valida la calidad de los requisitos, no la implementación.
**Propiedad de revisión**: Es un artefacto del revisor. Marcar `[x]` solo cuando el revisor determine que el criterio de calidad de requisitos está satisfecho.
**Semántica**: `[x]` significa que el requisito fue revisado y aprobado en calidad; no significa que la implementación esté terminada.

## Completitud de requisitos

- [ ] CHK001 ¿Están definidas todas las capacidades permitidas y prohibidas para cada actor institucional, función de proceso y cuenta técnica? [Completitud, Spec §Actores, FR-003]
- [ ] CHK002 ¿Están documentados los requisitos de cada recorrido I0–I6 desde interfaz hasta evidencia de aceptación sin depender de I7–I8? [Completitud, Spec §Escenarios, FR-040]
- [ ] CHK003 ¿Se especifican todos los datos mínimos exigidos para modelo, proceso, evidencia, decisión documental, valoración y cierre? [Completitud, Spec §Entidades clave]
- [ ] CHK004 ¿Están definidas las condiciones de creación, activación, cierre, reapertura, rectificación y sucesión para cada objeto versionado? [Completitud, Spec FR-009, FR-014, FR-019–FR-020, FR-024, FR-028, FR-032]
- [ ] CHK005 ¿Están definidos los requisitos de consulta y descarga de archivos además de carga, remisión e integridad? [Gap, Spec FR-017–FR-020]

## Claridad y ausencia de ambigüedad

- [ ] CHK006 ¿La diferencia entre recopilar, aceptar documentalmente y valorar académicamente está formulada sin términos que permitan inferencias entre etapas? [Claridad, Spec §Contexto, FR-023, FR-027]
- [ ] CHK007 ¿“Ámbito” tiene dimensiones y reglas de herencia explícitas para carrera, proceso e indicador? [Claridad, Spec FR-002, FR-013]
- [ ] CHK008 ¿“Operaciones rechazadas relevantes” está delimitado mediante una lista o criterio objetivo de auditoría? [Ambigüedad, Spec FR-006]
- [ ] CHK009 ¿“Otras condiciones configuradas de cierre” tiene procedencia, versionado y límites definidos? [Ambigüedad, Spec FR-031]
- [ ] CHK010 ¿Los conceptos “vigente”, “válida” y “completa” poseen criterios objetivos en todos los requisitos que los usan? [Ambigüedad, Spec FR-013, FR-018, FR-026, FR-031]

## Consistencia del dominio

- [ ] CHK011 ¿Los estados de evidencia son consistentes entre historias, requisitos, modelo de datos y efectos de una decisión documental? [Consistencia, Spec US4–US5, FR-019–FR-024]
- [ ] CHK012 ¿Las reglas de rectificación son consistentes para decisiones documentales, valoraciones académicas y reapertura del proceso? [Consistencia, Spec FR-015, FR-024, FR-028]
- [ ] CHK013 ¿La terminología de usuario, persona, actor, función, rol técnico y asignación se utiliza de forma canónica y no intercambiable? [Consistencia, Spec §Actores, §Entidades clave]
- [ ] CHK014 ¿Las prioridades P0/P1/P2 son coherentes con la declaración I0–I6 MVP e I7–I8 fuera del MVP? [Consistencia, Spec §Contexto y alcance, US1–US9]
- [ ] CHK015 ¿Los requisitos de informe interno evitan cualquier contradicción con las exclusiones de acreditación o acceso externo? [Consistencia, Spec FR-033–FR-034, §Exclusiones]

## Calidad de criterios de aceptación

- [ ] CHK016 ¿Cada requisito FR-001–FR-040 puede vincularse a por lo menos un escenario o resultado observable? [Trazabilidad, Spec §Escenarios, §Criterios de éxito]
- [ ] CHK017 ¿SC-003 define actores, datos de inicio y final del cronometraje necesarios para medir los 45 minutos? [Measurabilidad, Spec SC-003]
- [ ] CHK018 ¿SC-007 define muestra, perfil de participantes y qué constituye “tarea principal” y “primer intento”? [Measurabilidad, Spec SC-007]
- [ ] CHK019 ¿Los criterios p95 especifican carga, dataset, calentamiento y entorno de referencia suficientes para reproducir la medición? [Measurabilidad, Spec RNF-006–RNF-008, SC-008]
- [ ] CHK020 ¿SC-009 define cómo se demuestra el vínculo entre cada regla crítica y su prueba automatizada? [Trazabilidad, Spec SC-009, FR-040]

## Cobertura de escenarios

- [ ] CHK021 ¿Están cubiertos los recorridos alternos de observación, rechazo, corrección, rectificación y reapertura con resultado esperado? [Cobertura, Spec US3–US6, §Casos límite]
- [ ] CHK022 ¿Se especifican estados vacío, carga y error para tableros, bandejas, historial e informes? [Gap, Spec US4–US7]
- [ ] CHK023 ¿Están documentadas las consecuencias de retirar una asignación mientras existe trabajo en borrador o pendiente? [Cobertura, Spec §Casos límite, FR-013]
- [ ] CHK024 ¿Se define qué ocurre cuando una evidencia aceptada es rectificada después de una valoración final o un cierre? [Cobertura, Spec §Casos límite, FR-024, FR-028, FR-032]
- [ ] CHK025 ¿Los requisitos declaran que una falla total de I7/I8 no cambia ni bloquea ningún escenario I0–I6? [Cobertura, Spec FR-035, FR-038, SC-010]

## Casos límite y recuperación

- [ ] CHK026 ¿Las reglas de conflicto concurrente distinguen borradores, publicación, remisión, decisión, valoración y cierre? [Cobertura, Spec §Casos límite, RNF-011]
- [ ] CHK027 ¿Los requisitos de recuperación describen el estado esperado de metadatos, temporales y archivos tras cada punto de interrupción? [Cobertura, Spec RNF-010]
- [ ] CHK028 ¿Se define el comportamiento ante archivo duplicado, huella coincidente, referencia externa inaccesible o tipo detectado distinto del declarado? [Gap, Spec RNF-004, FR-017–FR-018]
- [ ] CHK029 ¿Se documenta el tratamiento de cero indicadores aplicables, fórmula con denominador cero y proceso sin evidencia? [Gap, Spec CL-006, FR-029]

## Requisitos no funcionales y seguridad

- [ ] CHK030 ¿Las reglas de autorización cubren lectura, búsqueda, agregados, exportación, descarga y auditoría además de mutaciones? [Cobertura, Spec FR-002, FR-030, FR-034, RNF-012]
- [ ] CHK031 ¿La política de sesión especifica renovación, sesiones simultáneas y comportamiento ante cambio de contraseña? [Gap, Spec RNF-002]
- [ ] CHK032 ¿Los tipos de archivo permitidos, detección de contenido y tratamiento de archivos peligrosos están definidos sin depender solo de la extensión? [Seguridad, Spec RNF-004]
- [ ] CHK033 ¿La accesibilidad cubre mensajes de error, progreso de carga, tablas, gráficos e historial para todos los recorridos I0–I6? [Cobertura, Spec RNF-014]
- [ ] CHK034 ¿La compatibilidad de navegadores es consistente con la política Baseline de la versión Angular seleccionada o existe una excepción explícita? [Consistencia, Spec RNF-015, Plan §Contexto técnico]

## Dependencias, supuestos y decisiones abiertas

- [ ] CHK035 ¿Cada CL-001–CL-010 identifica dueño institucional, fecha objetivo, fuente y efecto sobre requisitos si la decisión cambia? [Gap, Spec §Decisiones institucionales abiertas]
- [ ] CHK036 ¿Se distingue con claridad qué parámetros del piloto son supuestos reversibles y cuáles son requisitos constitucionales no negociables? [Claridad, Spec §Decisiones institucionales abiertas, §Supuestos]
- [ ] CHK037 ¿Los requisitos del puerto de almacenamiento son suficientes para que los adaptadores local y S3 preserven la misma semántica de integridad y recuperación? [Dependencia, Spec FR-017, RNF-010]
- [ ] CHK038 ¿Toda dependencia externa futura tiene comportamiento definido para ausencia, demora, duplicado y versión incompatible? [Cobertura, Spec FR-035–FR-038]

## Trazabilidad y conflictos

- [ ] CHK039 ¿Existe una relación explícita y sin huecos de cada historia a FR/RNF/SC, contrato, tarea y prueba? [Trazabilidad, Spec FR-040, Traceability]
- [ ] CHK040 ¿Los requisitos y el plan coinciden en que I7–I8 no son dependencias del MVP ni puertas para aceptar I0–I6? [Consistencia, Spec SC-010, Plan §Fases]

## Notas

- Profundidad: estándar, con énfasis de riesgo en seguridad por ámbito, separación del dominio, inmutabilidad y trazabilidad.
- Actor/momento: revisión humana previa a implementación y durante PR de artefactos SDD.
- Marcar `[x]` únicamente después de que el revisor confirme la calidad del requisito.
- Dejar sin marcar cuando haga falta aclaración, corrección o evidencia documental.
- `$speckit-implement` puede leer el estado, pero no debe modificar los marcadores.
