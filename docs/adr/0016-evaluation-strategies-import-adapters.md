# ADR-0016: Estrategias de valoración y adaptadores de importación

**Estado**: Aceptado
**Fecha**: 2026-10-19

## Contexto

Los modelos pueden declarar mecanismos de valoración diferentes. Algunos se resuelven mediante escalas y decisiones manuales configurables; otros podrían requerir algoritmos automáticos verificados. Además, futuras importaciones pueden provenir de documentos, formatos o servicios externos incompatibles entre sí.

Usar el mismo patrón para ambas variaciones confundiría dos problemas: seleccionar comportamiento interno y traducir representaciones externas. Tampoco es seguro ejecutar una fórmula arbitraria almacenada como datos.

## Decisión

### Strategy con Factory/Registry en I7

- `EvaluationStrategy` define el contrato para ejecutar un mecanismo de valoración sobre una entrada interna validada y producir un resultado trazable.
- Cada estrategia tiene clave estable, versión, mecanismos compatibles y estado de validación.
- Una Factory/Registry resuelve la estrategia por clave y versión; rechaza claves ausentes, versiones incompatibles o implementaciones no validadas.
- Las reglas configurables simples y la valoración manual del MVP no requieren una clase Strategy propia.
- No se interpreta ni ejecuta código, expresiones o scripts arbitrarios almacenados en el catálogo.

### Adapter en I8

- Se crea un adaptador solo cuando un formato de importación o servicio externo difiere del contrato interno.
- Cada adaptador traduce y valida su representación externa antes de entregar comandos o paquetes canónicos al dominio.
- La selección entre Medicina, Enfermería, Odontología, Derecho y General no usa Adapter; se resuelve consultando el catálogo y la versión asociada al proceso.
- El paquete completo se valida antes de aplicar cambios y una falla no modifica parcialmente el catálogo ni el proceso.

## Consecuencias

- Los algoritmos distintos pueden evolucionar sin condicionales por carrera y quedan sujetos a registro, versión y validación.
- Los formatos externos quedan aislados del modelo de dominio y pueden probarse con contratos independientes.
- El registro de estrategias agrega gobierno operativo y pruebas de compatibilidad.
- Cada adaptador agrega mantenimiento, observabilidad y una matriz de versiones; no debe crearse hasta existir una fuente externa concreta.
- I0–I6 funciona sin Strategy, importadores ni servicios externos.

## Alternativas descartadas

- **Condicionales por carrera**: acoplan algoritmos al nombre del programa y crecen de forma no verificable.
- **Una Strategy por indicador configurable**: multiplica clases sin que exista variación real de comportamiento.
- **Adapter para seleccionar modelos internos**: aplica un patrón de frontera a un problema de configuración ya resuelto por el catálogo.
- **Motor de expresiones arbitrarias en el MVP**: amplía la superficie de seguridad y permitiría ejecutar reglas no registradas.
- **Importador universal con condicionales de formato**: concentra acoplamiento y hace difícil validar cada contrato externo.

## Trazabilidad

FR-035–FR-038 y FR-041; tareas históricas T110–T121 y plan canónico de `specs/005-complete-evaluation-workflows/tasks.md`.
