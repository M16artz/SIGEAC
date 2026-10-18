# Guía de validación rápida

Esta guía valida el diseño y, después de la implementación, el recorrido completo I0–I6 con datos sintéticos. Docker es opcional.

## Prerrequisitos

- JDK 25 LTS y Maven Wrapper del repositorio.
- Node.js compatible con Angular 22 y npm mediante lockfile.
- PostgreSQL 18 accesible localmente; base y usuario exclusivos de prueba.
- Quarto para renderizar documentación.
- Directorio local escribible para archivos sintéticos, fuera de PostgreSQL.

## Variables locales

Configurar mediante archivo local no versionado o variables del proceso:

```text
SEC_DB_URL=jdbc:postgresql://localhost:5432/sec_unl
SEC_DB_USER=sec_app
SEC_DB_PASSWORD=<secreto-local>
SEC_FILE_ROOT=<directorio-local-dedicado>
SEC_DEMO_DATA=true
```

No utilizar `.env` compartido como fuente de secretos ni incluir credenciales en documentación o fixtures.

## Preparación sin Docker

1. Crear base y usuario local con privilegios limitados.
2. Ejecutar migraciones y pruebas backend: `./mvnw verify` desde `backend/` (`mvnw.cmd verify` en Windows).
3. Instalar dependencias bloqueadas y probar frontend: `npm ci` y `npm test -- --run` desde `frontend/`.
4. Ejecutar pruebas E2E con la base local: `npm run e2e` desde la raíz o el script que defina `tasks.md`.
5. Renderizar documentación: `quarto render` desde la raíz.

Testcontainers puede acelerar pruebas si hay un runtime compatible, pero nunca es el único camino.

## Escenario I0 — acceso

1. Cargar fixtures sintéticos de administrador, responsable, revisor, evaluador y coordinador.
2. Iniciar sesión con cada función y consultar `/api/v1/me`.
3. Intentar una operación permitida y otra fuera de carrera/proceso/indicador.

**Esperado**: la permitida se completa; la otra devuelve un Problem Detail seguro y genera auditoría. Una asignación responsable+revisor sobre la misma evidencia se rechaza salvo excepción vigente.

## Escenario I1 — modelo

1. Crear el modelo sintético con criterio, estándar, indicador, escala y fórmula.
2. Publicar la versión 1 con aprobador distinto del autor.
3. Intentar modificarla y luego crear versión 2 como sucesora.

**Esperado**: la versión 1 permanece idéntica; la modificación directa falla; la versión 2 conserva relación y procedencia.

## Escenario I2 — proceso

1. Crear la carrera sintética y un proceso ligado a modelo v1.
2. Asignar responsables por indicador y activar.
3. Publicar modelo v2 y comprobar la referencia del proceso.

**Esperado**: el proceso sigue usando v1; solo asignados operan sobre sus indicadores; cambios quedan auditados.

## Escenario I3 — evidencia

1. Crear borrador con procedencia y período.
2. Cargar un archivo sintético permitido menor de 25 MiB y remitir.
3. Intentar sobrescribir el archivo remitido; después crear versión sucesora.

**Esperado**: se registra huella SHA-256; no existe binario en PostgreSQL; la versión remitida no cambia y la sucesora preserva historial.

## Escenario I4 — revisión documental

1. Como revisor asignado, aceptar una versión remitida con justificación.
2. Consultar el indicador antes de una valoración.
3. Rectificar mediante una nueva decisión.

**Esperado**: aceptación no crea nivel académico; ambas decisiones permanecen y solo la revisión vigente gobierna elegibilidad.

## Escenario I5 — valoración académica

1. Intentar finalizar usando evidencia no aceptada.
2. Elegir evidencia aceptada, opción de escala sintética y justificación.
3. Rectificar con una nueva revisión.

**Esperado**: el primer intento falla; la valoración final conserva sustentos exactos; la revisión anterior no se sobrescribe.

## Escenario I6 — seguimiento y cierre

1. Consultar avance y contrastar conteos con registros fuente.
2. Intentar cerrar con un indicador obligatorio pendiente.
3. Completarlo, cerrar y solicitar informe.

**Esperado**: se muestran tres porcentajes y versión de fórmula; el cierre incompleto falla; el cierre válido congela un resumen reproducible y el informe declara que no es acreditación.

## Escenarios no funcionales

- Ejecutar matriz completa función×ámbito×operación y exigir 100 % de rechazos seguros fuera de ámbito.
- Ejecutar carga k6 con el dataset RNF-008 y conservar percentiles para RNF-006/RNF-007.
- Ejecutar axe/Playwright y navegación solo por teclado en recorridos I0–I6.
- Ejecutar el protocolo de `tests/usability/i0-i6-protocol.md` con al menos 20 participantes sintéticos de los cinco perfiles y exigir 19 tareas completadas al primer intento conforme SC-007.
- Interrumpir una carga durante promoción y ejecutar reconciliación; no debe aparecer evidencia remitida con archivo inexistente.
- Aplicar migraciones desde base limpia y desde la versión anterior; documentar respaldo y restauración.
- Deshabilitar cualquier adaptador I7/I8 y repetir I0–I6 sin degradación funcional.

## Resultado y evidencia

Guardar resultados bajo `tests/acceptance/results/` sin datos reales: versión de código, fecha, entorno, requisitos cubiertos, comandos, resultado y vínculos a logs sanitizados. Actualizar [traceability.md](./traceability.md) y la documentación Quarto antes de aceptar cada incremento.
