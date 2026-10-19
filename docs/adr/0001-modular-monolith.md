# ADR-0001: Monolito modular por capacidades

**Estado**: Aceptado
**Fecha**: 2026-10-18

## Contexto

I0–I6 comparten transacciones, identidad y trazabilidad, pero requieren límites estrictos entre recopilación, revisión documental y valoración académica.

## Decisión

Usar un único backend desplegable organizado por módulos de capacidad. Cada módulo expone puertos públicos y separa `api`, `application`, `domain` e `infrastructure`. ArchUnit impide dependencias hacia detalles internos. El módulo de integración es periférico y el MVP no depende de él.

## Consecuencias

- Consistencia transaccional simple y operación adecuada al piloto.
- Límites comprobables sin infraestructura distribuida.
- Una separación futura requiere respetar primero los puertos existentes.
- Toda excepción de dependencia necesita un ADR adicional.

## Alternativas descartadas

- Microservicios: complejidad operativa injustificada y contraria a la constitución.
- Capas técnicas globales: facilitarían acceso indiscriminado entre dominios.
