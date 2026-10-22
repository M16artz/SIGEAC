# Quickstart de validación: P01–P16

**Estado**: guía de aceptación objetivo. Los pasos funcionales se ejecutan al disponer de cada entrega; no constituyen resultados ya superados. La validación documental usa el lint del [contrato](contracts/README.md#validación-reproducible) y `quarto render`.

## Prerequisites

- Base limpia o migrada desde el último incremento aceptado.
- Datos exclusivamente sintéticos.
- Cinco configuraciones representativas y una carrera piloto.
- Usuarios distintos para administración, recopilación, revisión, evaluación, aprobación y mejora.
- Política sintética de competencias y aplicabilidad claramente rotulada. El piloto no presume vigencia institucional de los documentos aportados.

La preparación local y los comandos de backend/frontend están en la [guía común](../../docs/technical/quickstart.md). Validar contratos y migraciones con la [guía de transición](../../docs/technical/migration-p01-p16.qmd) antes de probar operaciones nuevas. No ejecutar los casos sobre datos institucionales ni marcar tareas por haber leído esta guía.

## A. MVP documental P01–P11

1. Crear unidad y carrera; comprobar código duplicado y desactivación histórica.
2. Publicar General 2024 y cuatro estructuras específicas mediante el catálogo común.
3. Crear proceso en preparación; comprobar que no se activa sin autorización.
4. Registrar autorización, activar, asignar responsables y crear actividades con fechas.
5. Configurar cuatro fuentes aplicables; remitir evidencias que cubran tres.
6. Aceptar dos fuentes, observar una y comprobar que ninguna decisión crea valoración.
7. Verificar 75 % de recopilación y 50 % de revisión documental.
8. Crear un corte documental y consultar todas sus referencias históricas.
9. Crear un borrador sucesor de una evidencia aceptada: la aceptación previa sigue contando. Remitir la sucesora: comprobar que necesita revisión y que no hereda aceptación.
10. Repetir con una evidencia que cubra dos fuentes; aceptar una y observar otra. Solo la aceptada suma revisión. Repetir con cero fuentes y con fuentes excluidas autorizadamente para distinguir causas `null`.

**Expected**: SC-011, SC-012, SC-013 y SC-017 cumplen con P12–P15 deshabilitados.

## B. Valoración P12

1. Registrar una valoración cualitativa sustentada.
2. Capturar variables verificadas y ejecutar una regla cuantitativa validada.
3. Intentar ejecutar una regla no validada y omitir una variable obligatoria.

**Expected**: solo los dos primeros recorridos generan resultados; todos conservan reglas y entradas.

## C. Hallazgos e informes P13–P14

1. Registrar fortaleza y debilidad vinculadas a resultados.
2. Generar informe preliminar, socializarlo y registrar observaciones.
3. Crear versión final, devolverla, corregirla y aprobarla.

**Expected**: cada versión y decisión es trazable; ninguna pantalla o exportación usa “acreditado”.

## D. Mejora, cierre y archivo P15–P16

1. Crear una acción desde la debilidad con responsable, plazo, meta y verificación.
2. Registrar avance, modificar mediante revisión y comprobar estado vencido.
3. Intentar cierre integral con pendientes; registrar una excepción autorizada o completarlos.
4. Cerrar, archivar, restaurar y consultar el proceso.
5. Crear otro proceso para la misma carrera.

El seguimiento de acciones posteriores al cierre sigue la política configurada y no reescribe la instantánea. Los escenarios exactos son AC-P15 y AC-P16 en la [matriz de aceptación](../../docs/testing/process-acceptance.md).

**Expected**: el histórico anterior no cambia y todas las decisiones permanecen recuperables.

## Evidence to preserve

- resultados de pruebas y comandos;
- versiones de contratos y migraciones;
- matrices de permisos y conflictos;
- cálculos manuales comparados con el tablero;
- hashes antes y después de respaldo/restauración;
- acta de revisión humana de competencias, textos de no acreditación y datos sintéticos.

