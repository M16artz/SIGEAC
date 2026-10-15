<!--
Sync Impact Report
==================
- Version change: 2.0.0 → 3.0.0
- Rationale: MAJOR — reemplazo completo de los 10 principios previos por 15
  principios nuevos y cambio de stack incompatible (Python/Streamlit/SimPy
  como arquitectura de producción → Java 21/Spring Boot + Angular + Kotlin).
- Modified principles (old → new):
  - I. Tecnologías → I. Stack Tecnológico (redefinido: Java 21 LTS, Spring
    Boot, PostgreSQL, OpenAPI, Maven, Structurizr DSL, Quarto, Graphify;
    Python/Streamlit pasan a implementación heredada)
  - II. Arquitectura → IV. Arquitectura (redefinido: módulos Java
    domain/application/simulation/analysis/infrastructure/api, monolito
    modular)
  - III. Simulación y Matemáticas → V. Modelo de Simulación (redefinido:
    entidades del restaurante, cancelación de ejecuciones, réplicas)
  - IV. Contratos → VI. Contratos (redefinido: 6 interfaces de contrato,
    campos expandidos, OpenAPI)
  - V. Ejecución Manual Obligatoria → XI. Ejecución bajo Autorización
    (renombrado/redefinido)
  - VI. CI/CD → X. CI/CD (redefinido: triggers prohibitivos ampliados,
    mínimo privilegio)
  - VII. Datos y Credenciales → XII. Datos Sintéticos y Credenciales
    (redefinido; ruta de credenciales: docs/test-credentials.md →
    docs/security/test-credentials.md)
  - VIII. Calidad → XIII. Calidad y Pruebas (redefinido: matriz de pruebas,
    Clean Architecture, regla "no afirmar pruebas pasadas sin ejecución")
  - IX. Skills y Documentación → XV. Skills + IX. Documentación con Quarto
    (dividido/expandido)
  - X. Gobernanza → XIV. SDD y Gobernanza (redefinido: flujo SDD completo,
    ADRs, cierre de fase en 9 pasos)
- Added principles:
  - II. Desarrollo Multiagente
  - III. Orquestación de Agentes
  - VII. Diagram as Code y C4
  - VIII. Graphify
- Added sections:
  - (párrafo preámbulo: constitución como fuente de autoridad normativa para
    Gemini, Codex, OpenCode, GitHub Copilot y Claude)
- Removed sections: ninguna (se conservan Contexto y Alcance del Proyecto y
  Flujo de Desarrollo, actualizadas).
- Contradicciones con la configuración anterior (v2.0.0) y ajustes propuestos:
  1. Stack: v2.0.0 exigía Python/SimPy/Streamlit como stack principal;
     v3.0.0 los prohíbe en la arquitectura de producción y los clasifica como
     implementación heredada (conservar, no migrar sin autorización).
     Ajuste: ninguna acción inmediata; la migración exige spec + contratos +
     pruebas de equivalencia + plan de transición aprobado.
  2. Credenciales: v2.0.0 ubicaba credenciales en docs/test-credentials.md;
     v3.0.0 usa docs/security/test-credentials.md.
     Ajuste pendiente (diferido): crear docs/security/test-credentials.md si
     se necesitan usuarios ficticios.
  3. Frontend: v2.0.0 definía Streamlit como interfaz web; v3.0.0 define
     Angular (TypeScript) + Kotlin/Jetpack Compose consumiendo API REST
     versionada vía OpenAPI.
  4. Artefactos nuevos requeridos por v3.0.0 y aún inexistentes en el repo
     (diferidos, fuera del alcance de este comando):
     - Archivos multiagente: AGENTS.md, CLAUDE.md, GEMINI.md,
       .github/copilot-instructions.md, configuración de OpenCode.
     - docs/architecture/c4/ (Structurizr DSL).
     - docs/quarto/ (.qmd técnicos y de usuario).
     - Directorio de artefactos de Graphify (política de versionado).
     - Criterios de aceptación arquitectónicos.
  5. README.md: conserva comandos de instalación de Angular (compatible) pero
     también instrucciones de specify con integración "copilot"; no contradice
     v3.0.0. Sin ajuste.
- Follow-up TODOs: ningún marcador de plantilla pendiente; los artefactos
  listados en el punto 4 son intents diferidos (ver Next Actions).
- Language: español (continúa la decisión de 2026-10-15).
- Ratification date: 2026-10-15 (se conserva la adopción original).
-->

# Constitución de SIGEAC — Optimización de la distribución de mesas, meseros y cocineros en un restaurante mediante redes de colas y simulación computacional

Esta constitución es la fuente de autoridad normativa del repositorio para
los agentes Gemini, OpenAI Codex, OpenCode, GitHub Copilot y Claude Code.

## Core Principles

### I. Stack Tecnológico

- Backend: Java 21 LTS y Spring Boot.
- Frontend web: Angular con TypeScript.
- Aplicación móvil Android: Kotlin y Jetpack Compose.
- Base de datos: PostgreSQL.
- API REST documentada mediante OpenAPI.
- Motor de simulación de eventos discretos en Java.
- Pruebas backend: JUnit 5 y Mockito.
- Pruebas frontend: herramientas oficiales del proyecto.
- Build backend: Maven; build frontend: npm.
- CI/CD: GitHub Actions.
- Diagramas C4 como código mediante Structurizr DSL.
- Documentación técnica y de usuario con Quarto.
- Mapeo estructural del repositorio mediante Graphify.
- PROHIBIDO utilizar Python ni Streamlit como tecnologías de la nueva
  arquitectura de producción.
- Los módulos existentes en Python se tratan como implementación heredada y
  referencia de comportamiento: NO DEBEN eliminarse ni migrarse sin
  autorización expresa.
- La migración a Java REQUIERE especificaciones, contratos, pruebas de
  equivalencia y un plan de transición aprobado.

**Rationale**: un stack único de producción elimina ambigüedad de
integración; tratar Python como herencia protege el comportamiento de
referencia mientras la migración se valida con evidencia.

### II. Desarrollo Multiagente

- El desarrollo DEBE poder realizarse con Gemini CLI, OpenAI Codex, OpenCode,
  GitHub Copilot y Claude Code.
- Todos los agentes DEBEN compartir las mismas especificaciones, contratos,
  decisiones arquitectónicas y restricciones.
- Mantener archivos de instrucciones compatibles cuando corresponda:
  `AGENTS.md`, `CLAUDE.md`, `GEMINI.md`,
  `.github/copilot-instructions.md` y los archivos de configuración
  específicos de OpenCode.
- La constitución y las especificaciones oficiales son la fuente de verdad.
- Los archivos de instrucciones particulares solo podrán adaptar el
  comportamiento a cada herramienta; NO podrán crear reglas contradictorias.
- Antes de modificar código, cada agente DEBE consultar, en orden:
  1. Constitución vigente.
  2. Especificación de la funcionalidad.
  3. Plan de implementación.
  4. Contratos afectados.
  5. Arquitectura C4.
  6. Mapa del repositorio, si existe y está actualizado.
  7. Documentación del módulo.
  8. Pruebas y dependencias relacionadas.

**Rationale**: cinco agentes distintos solo convergen si la fuente de verdad
es única y la lista de consulta es verificable antes de cada escritura.

### III. Orquestación de Agentes

- La colaboración multiagente se basa en responsabilidades explícitas.
- Roles lógicos: Arquitecto (diseño, C4, decisiones técnicas), Backend (Java,
  simulación, servicios), Frontend (Angular), Móvil (Kotlin/Android), QA
  (pruebas, contratos, criterios de aceptación), Documentador (Quarto),
  Integrador (consistencia y conflictos).
- Cualquier agente compatible podrá asumir cualquier rol: NO se establece que
  un proveedor sea obligatorio para un rol determinado.
- Cada tarea tendrá un responsable principal; se permiten revisores
  secundarios independientes.
- PROHIBIDO permitir escrituras simultáneas sobre los mismos archivos sin
  coordinación.
- Usar tareas pequeñas, ramas independientes y criterios explícitos de
  integración.
- Registrar por tarea: identificador, agente responsable, archivos
  modificados, contratos afectados, decisiones tomadas, pruebas propuestas o
  ejecutadas, estado de revisión y documentación actualizada.
- Un agente NO PODRÁ afirmar que otro agente revisó, validó o ejecutó código
  sin evidencia verificable.
- Si el entorno no permite ejecutar múltiples agentes, NO DEBE simularse
  colaboración inexistente: preparar las tareas para ejecución secuencial.

**Rationale**: la trazabilidad por tarea reemplaza la confianza implícita; el
registro es la evidencia que permite auditar quién hizo qué.

### IV. Arquitectura

- Arquitectura modular con separación estricta de responsabilidades.
- Backend Java con módulos: `domain`, `application`, `simulation`,
  `analysis`, `infrastructure`, `api`.
- `domain` NO DEPENDERÁ de frameworks de presentación, persistencia ni
  transporte.
- `simulation` NO DEPENDERÁ de Angular, Kotlin ni HTTP.
- La lógica matemática DEBE implementarse y probarse independientemente de
  las interfaces.
- Angular y Kotlin consumirán contratos de aplicación a través de la API
  REST, con DTO versionados, validaciones y errores estructurados.
- PROHIBIDO duplicar lógica entre plataformas.
- NO se introducirán microservicios sin justificar necesidad, coste,
  complejidad y ventajas; se prefiere el monolito modular para el primer
  incremento.

**Rationale**: las reglas de dependencia por módulo hacen que el núcleo
matemático sea testeable sin infraestructura y que la migración heredada sea
comparable módulo a módulo.

### V. Modelo de Simulación

- Representar: grupos de clientes, mesas y capacidad de asientos, meseros
  compartidos, cocineros compartidos, pedidos, colas, llegadas, eventos,
  tiempos de atención, ocupación y liberación de recursos, y finalización
  del servicio.
- Garantizar: conservación de recursos, ausencia de dobles asignaciones,
  tiempos no negativos, estados consistentes, trazabilidad de eventos,
  semillas reproducibles, múltiples réplicas y resultados estadísticamente
  comparables.
- Usar distribuciones probabilísticas documentadas.
- Aplicar M/M/s y la ley de Little solo cuando se satisfagan sus supuestos
  matemáticos.
- Registrar limitaciones del modelo.
- La simulación podrá realizarse de manera síncrona o como trabajo
  asíncrono según su duración.
- Cada ejecución DEBE poder cancelarse y producir un estado verificable.
- PROHIBIDO presentar datos sintéticos como observaciones reales.

**Rationale**: los invariantes (conservación, no duplicación, estados
consistentes) son las propiedades que hacen auditable una optimización de
asignación de mesas, meseros y cocineros.

### VI. Contratos

- Definir contratos explícitos entre: dominio y simulación, simulación y
  análisis, aplicación e infraestructura, backend y Angular, backend y
  Kotlin, backend y persistencia.
- Cada contrato incluirá: nombre y versión, propósito, entradas y salidas,
  tipos y unidades, validaciones, precondiciones, postcondiciones, errores,
  ejemplos sintéticos y compatibilidad.
- Utilizar OpenAPI para los contratos HTTP.
- Mantener trazabilidad entre requisitos, implementaciones, contratos y
  pruebas.

**Rationale**: los seis pases de frontera son los únicos puntos donde un
cambio puede romper a otro componente; formalizarlos convierte la
compatibilidad en algo verificable.

### VII. Diagram as Code y C4

- La arquitectura DEBE modelarse como código, usando Structurizr DSL como
  fuente principal para C4.
- Mantener al menos: Nivel 1 System Context, Nivel 2 Container, Nivel 3
  Component; Nivel 4 Code cuando aporte valor.
- Los diagramas DEBEN representar fielmente el estado real de la
  implementación: PROHIBIDO inventar dependencias o componentes.
- El modelo C4 vive en el repositorio, bajo `docs/architecture/c4/`.
- Actualizar los diagramas cuando cambien: límites del sistema,
  contenedores, dependencias, componentes, contratos de comunicación o
  tecnologías principales.
- Vincular los diagramas con Quarto.
- NO renderizar ni ejecutar herramientas de diagramación sin autorización
  expresa.

**Rationale**: diagramas versionados como código se revisan en PR junto con
el código que representan; un diagrama desactualizado es peor que no tener
diagrama.

### VIII. Graphify

- Utilizar Graphify para mapear el repositorio completo, incluyendo cuando la
  herramienta lo soporte: archivos y módulos, clases e interfaces,
  dependencias, relaciones entre componentes, documentación, configuraciones,
  contratos, pruebas y elementos arquitectónicos.
- El grafo es una fuente auxiliar para comprensión, análisis de impacto y
  detección de dependencias: el código fuente y los contratos PREVALECEN ante
  discrepancias con el grafo.
- Distinguir relaciones extraídas de relaciones inferidas.
- Generar o actualizar el mapa: tras la inicialización autorizada del
  repositorio, después de modificaciones estructurales relevantes, al
  finalizar cada fase previa autorización, o cuando el usuario lo solicite.
- NO ejecutar Graphify automáticamente.
- Conservar los artefactos del mapa en un directorio documentado, con
  política de versionado definida.
- NO enviar información sensible del repositorio a servicios externos sin
  autorización.

**Rationale**: el mapa acelera el análisis de impacto entre módulos, pero
solo es fiable si se distingue lo extraído de lo inferido y si el código
prevalece.

### IX. Documentación con Quarto

- Quarto es obligatorio para documentación técnica y de usuario. Al finalizar
  cada fase se preparan dos conjuntos:
  - **A. Documentación técnica**: arquitectura, módulos implementados,
    diagramas C4, contratos, modelos matemáticos, decisiones
    arquitectónicas, dependencias, configuración, pruebas, limitaciones y
    registro de cambios.
  - **B. Documentación de usuario**: objetivo del sistema, requisitos para
    utilizarlo, instalación y configuración aplicable, uso del frontend
    Angular, uso de la aplicación Android, ejecución de simulaciones,
    interpretación de resultados, errores frecuentes y preguntas frecuentes.
- Mantener archivos fuente `.qmd` bajo `docs/quarto/`.
- La documentación DEBE ser clara, técnica cuando corresponda, reproducible,
  versionada, consistente con el código y actualizada según los cambios
  realizados.
- Cada fase DEBE tener un informe de cierre con: alcance, funcionalidades
  desarrolladas, contratos modificados, pruebas y evidencias disponibles,
  problemas conocidos, diagramas actualizados, cambios para el usuario y
  trabajo pendiente.
- Preparar la documentación antes de considerar una fase lista para cierre.
- NO ejecutar `quarto render` ni publicar documentación sin autorización
  expresa.
- Distinguir la documentación redactada de los documentos efectivamente
  renderizados y verificados.

**Rationale**: la fase no está terminada hasta que su evidencia (docs,
informe) existe; separar "redactado" de "renderizado verificado" evita
afirmaciones de completitud sin ejecución.

### X. CI/CD

- Implementar CI/CD mediante GitHub Actions, con workflows preparados para:
  validación de Java, pruebas de Spring Boot, validación de Angular, pruebas
  de Kotlin, revisión de contratos, análisis estático, revisión de
  seguridad, compilación, empaquetado, documentación Quarto y despliegue por
  entorno.
- Los workflows utilizarán exclusivamente `workflow_dispatch`.
- PROHIBIDO habilitar ejecuciones automáticas en `push`, `pull_request`,
  `schedule` o `release`.
- NO publicar artefactos ni desplegar sin autorización.
- Separar desarrollo, pruebas y producción.
- Aplicar el principio de mínimos privilegios.
- NO incluir secretos en el repositorio.

**Rationale**: los pipelines existen preparados, no activados; cada corrida
es una decisión humana, alineada con el principio XI.

### XI. Ejecución bajo Autorización

- NINGÚN agente podrá ejecutar automáticamente: instalaciones, comandos de
  terminal, scripts, tests, compilaciones, servidores, simulaciones,
  migraciones, renderizados, pipelines, despliegues u operaciones destructivas.
- Cada ejecución requerirá autorización explícita del usuario para la acción
  concreta.
- Se permite, sin ejecución operativa: analizar código y documentos, proponer
  modificaciones, redactar código, crear especificaciones, preparar
  configuraciones, generar documentación fuente y preparar comandos para
  revisión.
- La autorización para una tarea NO implica permiso permanente para ejecutar
  acciones futuras.

**Rationale**: los efectos secundarios (simulaciones, builds, despliegues)
son costosos o irreversibles; el control lo retiene quien responde por el
resultado.

### XII. Datos Sintéticos y Credenciales

- Utilizar datos sintéticos en desarrollo y pruebas; crear generadores
  reproducibles cuando sea necesario.
- Documentar del dataset sintético: origen sintético, distribuciones,
  parámetros, semillas y limitaciones.
- Si se necesitan usuarios ficticios, registrarlos en
  `docs/security/test-credentials.md` con: identificador de usuario
  ficticio, rol, entorno, propósito, permisos y procedimiento de creación.
- Contraseñas solo podrán documentarse cuando sean valores de demostración
  sin capacidad de acceso a sistemas o entornos reales.
- NO almacenar credenciales operativas en Markdown.
- Utilizar variables de entorno o almacenes de secretos para cualquier
  credencial válida.
- Crear `.env.example` sin valores sensibles y excluir los archivos de
  secretos del control de versiones.

**Rationale**: la distinción entre credencial de demostración y operativa es
la única defensa que no se puede "arreglar después" si un secreto se filtra
en el historial de Git.

### XIII. Calidad y Pruebas

- Aplicar: SOLID, DRY, KISS, Clean Architecture cuando aporte valor, tipado
  fuerte, validación explícita y gestión controlada de excepciones.
- Priorizar pruebas: unitarias, de integración, de contratos, de
  consistencia matemática, de regresión, de API, del frontend y de Android.
- Los agentes podrán preparar pruebas sin ejecutarlas.
- NO afirmar que una prueba pasó si no fue ejecutada.

**Rationale**: la simulación introduce variabilidad inherentemente; la
calidad del código y sus pruebas deben ser el factor determinista que la
contrarreste, y la evidencia de una prueba es su ejecución, no su existencia.

### XIV. SDD y Gobernanza

- Seguir Spec-Driven Development con el flujo: constitution → specify →
  clarify → plan → tasks → implement → review → documentation → phase
  closure.
- Toda funcionalidad DEBE vincularse a una especificación y criterios de
  aceptación.
- Todo cambio arquitectónico significativo DEBE registrarse mediante ADR.
- Todo cierre de fase requiere: 1) revisión de requisitos, 2) revisión de
  contratos, 3) revisión del código, 4) revisión de diagramas C4, 5) revisión
  del mapa Graphify, 6) documentación técnica Quarto, 7) documentación de
  usuario Quarto, 8) informe de cierre y 9) aprobación del usuario.
- NO dar por completada una fase cuando falten entregables obligatorios.

**Rationale**: el flujo SDD convierte cada entrega en un conjunto verificable
de artefactos; el cierre en 9 pasos impide declarar fases completas sin
evidencia.

### XV. Skills

- Permitir el uso de skills especializadas cuando estén disponibles y sean
  pertinentes, priorizando: Java y Spring Boot, Angular, Kotlin,
  arquitectura C4, testing, seguridad, Quarto, Graphify y gestión
  multiagente.
- NO asumir que todas las skills están instaladas.
- NO instalar ni ejecutar skills sin autorización.
- Las instrucciones de una skill NO PODRÁN modificar ni anular las
  restricciones de esta constitución.

**Rationale**: las skills son una aceleración sujeta a la constitución, no
una autoridad paralela.

## Contexto y Alcance del Proyecto

SIGEAC optimiza la distribución de mesas, meseros y cocineros en un
restaurante mediante redes de colas y simulación computacional. La
arquitectura de producción comprende: backend Java 21 LTS con Spring Boot
(incluido el motor de simulación de eventos discretos), frontend web Angular
(TypeScript), aplicación Android con Kotlin y Jetpack Compose, base de datos
PostgreSQL y API REST documentada con OpenAPI; CI/CD con GitHub Actions,
arquitectura modelada con Structurizr DSL (C4), documentación con Quarto y
mapa del repositorio con Graphify. Los módulos Python existentes se conservan
como implementación heredada y referencia de comportamiento. El desarrollo es
multiagente (Gemini, Codex, OpenCode, Copilot, Claude) y esta constitución es
su fuente de autoridad normativa.

## Flujo de Desarrollo (Spec-Driven Development)

- Flujo obligatorio: constitution → specify → clarify → plan → tasks →
  implement → review → documentation → phase closure (principio XIV).
- Antes de modificar código, cada agente DEBE realizar la consulta previa de
  8 puntos del principio II (constitución, spec, plan, contratos, C4, mapa
  del repositorio, documentación del módulo, pruebas y dependencias).
- Las tareas DEBEN ser pequeñas, con responsable principal, ramas
  independientes y registro de la información exigida en el principio III.
- Las puertas de calidad (análisis estático, lint, pruebas, builds,
  renderizados, pipelines) DEBEN prepararse y ejecutarse solo con
  autorización explícita (principios X y XI).
- El cierre de fase DEBE cumplir los 9 requisitos del principio XIV,
  incluida la documentación Quarto y la aprobación del usuario.

## Governance

- Esta constitución es la fuente de autoridad normativa del repositorio para
  los agentes Gemini, OpenAI Codex, OpenCode, GitHub Copilot y Claude Code;
  tiene prioridad sobre decisiones técnicas posteriores y sobre los archivos
  de instrucciones particulares, que solo pueden adaptar comportamiento sin
  crear reglas contradictorias.
- La constitución junto con las especificaciones oficiales forma la fuente de
  verdad; los conflictos se resuelven a su favor y toda desviación requiere
  una enmienda explícita.
- Enmiendas: se realizan únicamente editando `.specify/memory/constitution.md`
  mediante `/speckit.constitution`, siempre con Sync Impact Report y con
  aprobación del responsable del proyecto antes de commitear.
- Política de versionado (semver): MAJOR = eliminación o redefinición
  incompatible de principios o gobernanza; MINOR = principio o sección nueva,
  o expansión material de su guía; PATCH = clarificaciones y cambios no
  semánticos. La línea de versión del pie DEBE coincidir con el Sync Impact
  Report de la enmienda.
- Revisión de cumplimiento: cada PR y su revisor DEBEN verificar el
  cumplimiento de los quince principios; toda ejecución de comandos,
  pipelines, renderizados o despliegues DEBE respaldarse en una autorización
  explícita del usuario (principios X y XI); ningún agente DEBE afirmar
  ejecuciones o revisiones ajenas sin evidencia verificable (principio III);
  los ítems `TODO(...)` diferidos DEBEN rastrearse y resolverse o renovarse
  en cada enmienda.

**Version**: 3.0.0 | **Ratified**: 2026-10-15 | **Last Amended**: 2026-10-15
