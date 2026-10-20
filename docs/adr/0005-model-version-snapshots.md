# ADR-0005: Instantáneas de modelo y fórmula declarativa

**Fecha**: 2026-10-20  
**Estado**: Aceptada

## Decisión

Cada versión conserva una instantánea autocontenida de jerarquía, escala y fórmula. Solo un borrador admite reemplazo con revisión optimista. Publicar valida y congela la instantánea; corregir copia a una sucesora. La fórmula usa un esquema JSON allowlisted y nunca código ejecutable.

## Consecuencias

Se duplica contenido entre versiones a cambio de trazabilidad e inmutabilidad. I2–I6 consumen versiones publicadas por un puerto estable. Escalas, ponderaciones y tratamiento de no aplicables siguen siendo datos con procedencia y no decisiones CL-003/CL-005/CL-006 codificadas.
