<!--
Sync Impact Report
- Version change: template sin versión -> 1.0.0
- Modified principles: marcadores iniciales -> siete principios vinculantes del proyecto
- Added sections: Restricciones del producto y tecnología; Flujo de desarrollo y puertas de calidad
- Removed sections: ninguna; se sustituyeron todos los marcadores de la plantilla
- Follow-up TODOs: ninguno
-->
# Constitución del Sistema de Evaluación de Carreras

## Core Principles

### I. Separación estricta del dominio académico

El sistema DEBE representar como conceptos distintos la recopilación de información, la revisión
documental y la valoración académica del cumplimiento. Aceptar una evidencia documental NO DEBE
implicar que el estándar académico está cumplido. Los actores institucionales, los roles de proceso
y los roles técnicos DEBEN modelarse por separado. Esta separación evita atribuir competencias a
personas u órganos que no las poseen.

### II. Desarrollo guiado por especificaciones y trazabilidad

Todo incremento DEBE comenzar con una especificación verificable y mantener relaciones explícitas
entre requisitos, historias, casos de uso, contratos, tareas y pruebas. Ninguna funcionalidad P0
DEBE implementarse sin criterio de aceptación identificable. Los documentos normativos son fuentes
del dominio, no autorización para inventar reglas institucionales; toda interpretación pendiente
DEBE registrarse como decisión abierta y conservar su procedencia.

### III. Incrementos verticales e independientes

Cada incremento DEBE entregar un recorrido comprobable que incluya, cuando corresponda, interfaz,
contrato REST, lógica de aplicación, reglas de dominio, persistencia, migración y pruebas. Las
historias de un incremento DEBEN poder demostrarse sin depender de funcionalidades planificadas para
incrementos posteriores. I0 a I6 conforman el MVP; I7 e I8 NO DEBEN convertirse en dependencias del
MVP salvo en contratos de extensión expresamente documentados.

### IV. Seguridad por función y ámbito

Toda operación protegida DEBE autenticarse y autorizarse en el backend según función, carrera,
proceso e indicador cuando aplique. El frontend NO DEBE ser la autoridad de permisos. Las cuentas
técnicas NO DEBEN adquirir capacidad para alterar decisiones académicas por privilegio de
infraestructura. Contraseñas, archivos y datos de prueba DEBEN cumplir RNF-001 a RNF-005 y RNF-019;
las excepciones de separación de funciones DEBEN ser explícitas, autorizadas y auditadas.

### V. Inmutabilidad, versionado y auditoría

Una versión de modelo utilizada por un proceso y una versión de evidencia remitida a revisión DEBEN
permanecer identificables e inmutables. Corregir una evidencia DEBE crear una nueva versión. Toda
decisión de revisión y todo cambio crítico en modelos, permisos, asignaciones o evidencias DEBE
registrar actor, fecha, acción y objeto afectado. Las migraciones DEBEN ser reproducibles y preservar
el historial.

### VI. Arquitectura modular comprobable

La solución DEBE iniciar como aplicación cliente-servidor con un backend monolítico modular. El
backend DEBE organizarse por capacidades funcionales y separar presentación, aplicación, dominio e
infraestructura. Los controladores NO DEBEN contener reglas del dominio; los repositorios NO DEBEN
decidir autorizaciones académicas; los módulos NO DEBEN acceder indiscriminadamente a detalles
internos de otros módulos. Toda excepción de dependencia DEBE justificarse mediante ADR.

### VII. Calidad verificable y documentación viva

Las reglas críticas de estados, permisos, cálculo de avance, versionado y conflictos de funciones
DEBEN tener pruebas automatizadas. Cada incremento DEBE actualizar la matriz de trazabilidad, los
contratos modificados, las decisiones técnicas pertinentes y la documentación técnica y de usuario.
Los objetivos de rendimiento DEBEN medirse en un entorno reproducible. Ningún incremento se considera
terminado mientras existan incumplimientos críticos de su especificación o de esta constitución.

## Restricciones del producto y tecnología

- El producto es un apoyo a la autoevaluación; NO emite acreditaciones, NO sustituye comisiones
  oficiales y NO presume acceso directo de CACES o CES.
- El MVP DEBE usar datos sintéticos y una carrera piloto, un modelo versionado y un proceso de
  autoevaluación. Las decisiones CL-001 a CL-010 permanecen sujetas a validación institucional.
- La base tecnológica aprobada para planificación es Angular, Java LTS con Spring Boot, PostgreSQL,
  Spring Data JPA/Hibernate, Spring Security, Flyway y OpenAPI 3.x.
- Los archivos DEBEN almacenarse fuera de PostgreSQL; la base conserva metadatos, integridad y
  referencias. El adaptador concreto puede ser local o compatible con S3.
- El sistema DEBE ejecutarse en los entornos documentados sin depender de Docker. Docker puede ser
  opcional, pero nunca el único camino de desarrollo o prueba.
- La API REST es el contrato común para el cliente web y futuras integraciones. Kotlin, servicios
  institucionales, notificaciones y despliegue productivo quedan fuera del MVP.
- GitHub Actions DEBE ejecutarse únicamente mediante activación manual mientras esta regla esté
  vigente; no se permiten despliegues automáticos.
- Quarto y los diagramas C4 como código son los formatos preferidos para documentación reproducible.

## Flujo de desarrollo y puertas de calidad

1. Cada incremento DEBE recorrer `specify -> clarify -> plan -> tasks -> implement -> verify`.
2. La especificación DEBE declarar alcance, exclusiones, actores, escenarios, requisitos y resultados
   medibles antes de planificar.
3. La planificación DEBE superar la comprobación constitucional y producir, cuando corresponda,
   investigación, modelo de datos, contratos y guía de validación rápida.
4. Las tareas DEBEN ordenarse por dependencia, agruparse por historia y contener rutas concretas.
5. Las pruebas DEBEN escribirse para contratos y reglas críticas; un cambio no puede aceptarse si
   rompe incrementos previamente aceptados.
6. Toda migración DEBE probarse desde una base limpia y documentar respaldo/restauración cuando afecte
   datos persistentes o archivos.
7. Todo cambio de arquitectura, seguridad, modelo de datos transversal o contrato público DEBE incluir
   o actualizar un ADR.
8. Antes de implementar, el equipo DEBE ejecutar el análisis cruzado de especificación, plan y tareas,
   y resolver los hallazgos críticos.
9. La revisión humana DEBE confirmar requisitos, permisos, errores de API, trazabilidad, documentación
   y ausencia de datos institucionales reales no autorizados.

## Governance

Esta constitución prevalece sobre planes, tareas y convenciones locales que la contradigan. Una
enmienda requiere una propuesta documentada, evaluación de impacto sobre artefactos existentes y
aprobación explícita del equipo responsable. Los cambios se versionan con SemVer: MAJOR para retirar o
redefinir principios de forma incompatible, MINOR para añadir principios o ampliar obligaciones de
manera material y PATCH para aclaraciones sin cambio normativo.

Cada especificación y pull request DEBE incluir una comprobación de cumplimiento. Las desviaciones
temporales solo son válidas si se documentan con motivo, alcance, riesgo, responsable, fecha de revisión
y plan de eliminación. La complejidad adicional DEBE justificarse en el plan o en un ADR. La
constitución DEBE revisarse al cerrar cada incremento y antes de aceptar cambios que afecten seguridad,
trazabilidad o competencias institucionales.

**Version**: 1.0.0 | **Ratified**: 2026-10-18 | **Last Amended**: 2026-10-18
