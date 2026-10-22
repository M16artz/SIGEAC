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
