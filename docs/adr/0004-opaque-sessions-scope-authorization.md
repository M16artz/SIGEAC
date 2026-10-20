# ADR-0004: Sesiones opacas y autorización por ámbito

**Estado**: Aceptado  
**Fecha**: 2026-10-20

I0 usa cookies con tokens aleatorios opacos; solo su SHA-256 se persiste. Cada
operación reevalúa identidad activa, función, operación, ámbito, vigencia,
conflictos y excepciones. Las colecciones se filtran y los identificadores
directos ajenos se ocultan como inexistentes. BCrypt protege credenciales. Los
catálogos y conflictos son configuración sintética, nunca reglas CL fijas.

Esto permite revocación inmediata y evita que privilegios técnicos impliquen
competencias funcionales. Se descartan JWT autocontenidos por revocación tardía
y permisos codificados en controladores por acoplamiento y riesgo de omisión.
