# ADR-0015: Gestión configurable de múltiples modelos de evaluación

**Estado**: Aceptado
**Fecha**: 2026-10-19

## Contexto

El sistema debe representar un modelo general y modelos específicos de Medicina, Enfermería, Odontología y Derecho. Sus jerarquías, fuentes, períodos e indicadores pueden diferir y evolucionar. Crear una implementación por carrera duplicaría persistencia, servicios, validaciones y pruebas, dificultaría la trazabilidad de versiones y convertiría cada cambio normativo en un cambio de software.

Los modelos generales y específicos pueden compartir códigos o conceptos, pero esa coincidencia no autoriza a combinar sus indicadores. Cada proceso debe poder demostrar exactamente qué versión utilizó.

## Decisión

Usar un catálogo común, configurable y versionado de modelos de evaluación.

- Un modelo identifica organismo emisor, denominación, alcance general o específico y documento fuente.
- Una versión identifica número, fecha, autor, aprobador, estado y predecesora.
- La estructura se guarda como una jerarquía ordenada de nodos tipados capaz de expresar criterios, subcriterios, estándares, indicadores y futuras categorías sin crear tablas por carrera.
- Cada indicador declara fuentes de información, período de evaluación, naturaleza cualitativa o cuantitativa y mecanismo de valoración cuando corresponda.
- Cada fuente requerida posee identidad propia y cada versión de evidencia declara explícitamente cuáles cubre; esta relación alimenta las métricas documentales.
- Las reglas cuantitativas pertenecen al indicador e incluyen variables, unidades, fórmula, umbrales y estado de validación. La fórmula de avance documental no sustituye estas reglas.
- Una versión publicada es inmutable. Un proceso referencia una sola versión publicada y esa asociación no cambia durante su ciclo de vida.
- No existe fusión, herencia ni sustitución automática entre modelos. Toda composición futura requerirá una decisión explícita y otro ADR.

## Consecuencias

- Un mismo conjunto de servicios y pantallas administra modelos distintos.
- Las diferencias estructurales se validan con configuraciones y fixtures, no con implementaciones por carrera.
- La trazabilidad de procesos permanece estable aunque se publique una versión posterior.
- Las consultas jerárquicas, reglas de orden y restricciones de tipo son más complejas que un esquema rígido.
- Las pruebas de I2 deben demostrar estructuras representativas diferentes; no necesitan implementar ni verificar todos los cálculos académicos de los cinco modelos.

## Alternativas descartadas

- **Cinco implementaciones independientes**: duplica reglas y migraciones, y favorece divergencias no intencionales.
- **Un modelo general con extensiones implícitas por carrera**: dificulta saber qué indicadores pertenecen realmente a cada versión.
- **Documento JSON sin estructura relacional común**: simplifica la captura inicial, pero debilita integridad, consultas, referencias históricas y validaciones.

## Trazabilidad

FR-007–FR-012 y FR-042–FR-047; tareas históricas T040–T050/T052–T054 y plan canónico de `specs/005-complete-evaluation-workflows/tasks.md`.
