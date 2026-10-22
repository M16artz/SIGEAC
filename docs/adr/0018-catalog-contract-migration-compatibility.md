# ADR-0018 — Compatibilidad del catálogo y secuencia de migraciones

Fecha: 2026-10-21. Estado: adoptada para la entrega técnica D1; no acredita validación institucional de los modelos.

## Contexto

El contrato ejecutable usa DTO legados de modelos/procesos y revisiones desde cero.
005 propone DTO anidados, revisiones desde uno, equipos y decisiones más completas.
Ambos declaraban `/api/v1`, por lo que una mezcla directa rompería clientes.
Además, reservar V020 para carreras y dejar V008 pendiente de evidencias habría
obligado a introducir una migración inferior después de aceptar D1.

## Decisión

1. Conservar las rutas y DTO v1 existentes. Añadir el catálogo de carreras/unidades
   en v1, ya que no había rutas anteriores que reemplazar. Sus DTO usan `data`,
   `revision>=1`, `status`, `createdBy/createdAt` asignados por servidor y páginas
   `items/page/size/totalElements`. Se rechazan campos de comando desconocidos.
2. Declarar `/api/v2` como servidor del contrato **planificado** de 005; D1 lo
   sobrescribe explícitamente a v1 por ruta. Los demás cambios se implementarán
   con su migración de cliente por entrega. No se crea un backend v2 ficticio ni
   se activa un alias que acepte DTO incompatibles.
3. Compartir las rutas y esquemas D1 mediante referencias OpenAPI externas. El
   verificador de compatibilidad y los lints forman parte de la comprobación de
   contratos. Una única definición evita divergencia entre dos copias.
4. Mantener V001–V007 intactas y usar V008 para carreras, V009 para modelos,
   V010 para gobierno, V011 para inmutabilidad de evidencias y V012–V018 para
   coberturas, revisión, seguimiento, evaluación, informes, mejora y archivo.
   No habilitar `out-of-order`, `repair` ni migraciones vacías para resolver la reserva anterior.
5. Migrar UUID de carreras antiguas a registros pendientes de clasificación,
   deshabilitados y vinculados a una unidad técnica explícita. El actor cero
   identifica una operación técnica de migración, no una persona ni competencia
   institucional. No se inventan nombres de carreras, autoridades o autorizaciones.
6. `CAREER_ADMIN` es una capacidad de **catálogo global**, necesaria para crear
   identidades de carreras que aún no tienen asignaciones. No otorga capacidades
   de proceso, revisión, valoración o autorización institucional. `CAREER_READ`
   mantiene el filtrado por carrera, incluyendo el total paginado, unidades e
   historial. Una asignación limitada a un proceso no se amplía a toda su carrera.
7. El rol funcional existente recibe estas capacidades mediante migración; las
   funciones conocidas reciben lectura dentro de sus ámbitos. No se crea un
   comodín de carrera ni se asigna autoridad institucional a administradores.
   Roles personalizados deben configurarse expresamente. Las cuentas técnicas
   no adquieren administración funcional.
8. Las tablas de revisión son append-only. Desactivar no borra carreras ni
   procesos, no revoca silenciosamente permisos históricos y bloquea nuevos
   procesos/activaciones. Una unidad con carreras activas no puede desactivarse.
9. Incluir la integración `spring-boot-starter-flyway` para ejecutar migraciones
   antes de validar JPA en el arranque, conservando el módulo PostgreSQL y las
   versiones gestionadas por Spring Boot. La documentación oficial identifica
   este starter como integración de Flyway: [Database Initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html).
10. SQLite permanece auxiliar, con pool de una conexión para evitar conflictos
    de escritor durante la renovación de sesiones. La integración y las
    restricciones se verifican además sobre PostgreSQL; no se extrapolan a
    rendimiento concurrente los resultados de SQLite.

## Consecuencias y alternativas

- Los clientes de acceso/modelos/procesos existentes no cambian de base URL.
  D1 es una adición, mientras D2–D8 conservan un contrato objetivo separado.
- La aceptación técnica D1 no completa T002, T009 o T012 para todo P01–P16.
- Las referencias heredadas necesitan clasificación humana antes de uso nuevo;
  el piloto sintético conocido puede reconciliarse solo con datos demo habilitados.
- Se descartaron reemplazar silenciosamente DTO v1, usar un permiso global
  implícito por nombre de usuario, fabricar carreras institucionales desde UUID
  y aplicar migraciones fuera de orden.

## Verificación

`CareerCatalogMigrationTest`, `CareerCatalogStartupTest`, pruebas de dominio,
servicio/controlador, Angular y `tests/acceptance/career-catalog.spec.ts`.
Resultados y límites: [aceptación D1](../../tests/acceptance/results/d1-careers.md).
