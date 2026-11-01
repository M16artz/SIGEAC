# APE 5 — Integración, contenedorización y despliegue del backend

## Datos de Identificación del Estudiante y la Práctica

| Campo | Valor |
|---|---|
| Estudiante(s) | Pendiente de identidad/aporte reales. |
| Práctica / fecha | 005 / Semana5, fecha real pendiente. |
| Tiempo planificado | 3 horas. |
| Título | Integración, contenedorización y despliegue del backend, formulado desde la guía resumida aportada. |
| Asignatura, ciclo, unidad y docente | Referencias APE4: Desarrollo Basado en Plataformas, 5 A, 1, Edison Leonardo Coronel Romero; confirmar para APE5. |
| Horario y lugar | No especificados en la guía resumida de Semana5; pendientes, sin inventar. |
| Resultado de aprendizaje | No aportado específicamente para APE5; no se atribuye un texto nuevo a la guía. |

## Objetivo(s) de la Práctica

Integrar, contenedorizar y desplegar el backend; publicar documentación de API y evidencias de pruebas; mantener producto versionado, instrucciones reproducibles y contribución explicada. La guía se recibió resumida, por lo que esta organización desarrolla sus puntos sin inventar un procedimiento oficial numerado ni preguntas de control no aportadas.

## Materiales, Reactivos, Equipos y Herramientas

Repositorio, especificación/arquitectura/contratos/tests anteriores; Java/Spring/PostgreSQL del producto; modelo de amenazas y riesgos Semana4. Se elaboran Dockerfile, Compose, contexto restringido y ejemplo env. Motor Docker/Podman no disponible, imágenes no descargadas, no build ni despliegue. Seguridad/DRF/sobre y aceptación PostgreSQL siguen abiertos. No hay proveedor, credencial de publicación ni URL desplegada aportados.

## Procedimiento / Metodología Ejecutada

1. Revisar correspondencia de flujos/requisitos y límites de evidencia APE3/4.
2. Seleccionar empaquetado Spring **solo como propuesta para el backend existente**, sin resolver unilateralmente DRF ni adaptar cliente/contrato.
3. Preparar Dockerfile con imagen runtime por argumento, JAR explícito, usuario no root y puerto8080; el contexto admite solo app.jar y no incluye .git ni env del host.
4. Preparar Compose con DB PostgreSQL18 sin puerto público, red interna, volumen, healthcheck, límites y API loopback18095; secrets fuera de Git, prod/demo/cookie explícitos. Las imágenes deben fijarse por digest y existir localmente; no se afirma esa disponibilidad.
5. Documentar construcción offline/aislada, hash/commit, migraciones, fixture, TLS/cookie y permisos efectivos, backup/restauración y rollback de imagen/schema; nada se ejecuta por escribir la receta.
6. Referenciar documentación de contrato existente y plan de publicación estática separada. Prod deshabilita Swagger por configuración: no se activa automáticamente para satisfacer “publicar documentación”.
7. Preparar registro de estado de despliegue y checklist de aceptación; sin URL ni logs de arranque nuevos. No se usa la disponibilidad local temporal de APE3 como despliegue APE5.

[INSTRUCCIONES_DESPLIEGUE.md](INSTRUCCIONES_DESPLIEGUE.md) es la receta completa, con condiciones y comandos previstos. El motor debe validar Compose y permisos; revisar sintaxis YAML con una librería no equivale a `docker compose config`, build o aislamiento. Esta entrega mantiene la constitución: Docker no es único camino obligatorio y Actions no se dispara automáticamente.

La propuesta usa propietarioDB para migración/arranque por simplicidad del fixture y lo declara: no alcanza mínimo privilegio productivo. Se requiere separar migrador/runtime y probar permisos antes de publicar. Cookie Secure=true exige TLS/proxy para un recorrido de navegador: no desactivarla en prod para una demo. No se incluyó un proxy/certificado ficticio ni se afirma CORS configurado.

## Resultados

| Producto requerido | Evidencia disponible | Estado |
|---|---|---|
| Integración funcional | Código y resultados históricos APE3, matrices | Parcial; no nueva integración ejecutada. |
| Contenedorización | Dockerfile y Compose versionables, contexto allowlist | Artefactos preparados; no construidos/arrancados. |
| Despliegue / URL o evidencia | estado-despliegue.json declara ausencia | Pendiente; localhost previsto no es URL desplegada. |
| Instrucciones reproducibles | Receta con imágenes/digests, env, DB/files, pruebas y rollback | Documentadas, sin reproducción real certificada. |
| API y pruebas publicadas | Contratos locales y documentación existente; no hosting | Disponibles localmente, publicación pendiente. |
| Repositorio versionado/aporte | Archivos y manifiesto; no commit/PR | Contribución real por completar y versionar. |
| Seguridad básica | Configuración propuesta, controles previos y SEC pendientes | No certificada ni cerrada. |

## Preguntas de Control

La guía resumida de APE5 no aportó preguntas numeradas. No se inventan preguntas oficiales. Para revisar la reproducción: qué commit/JAR/image se desplegó, qué env/permisos fueron efectivos, qué flujo/pruebas pasaron, cómo persisten/restauran datos, y qué cambió cada integrante. Son preguntas de apoyo, no enunciados del docente.

## Conclusiones

Se preparó un paquete de despliegue que evita enviar secretos al contexto y explicita dependencias/configuración. **La práctica no está completamente realizada:** faltan build, despliegue, pruebas, publicación, correcciones de seguridad y evidencia individual versionada. Tener un Dockerfile no equivale a un backend desplegado.

## Recomendaciones

Resolver tecnología/contrato; verificar controles pendientes; disponer de sandbox/motor e imágenes auditadas; reconstruir artefacto del commit actual; ejecutar pruebas y migraciones PostgreSQL; validar TLS/mínimo privilegio y restauración; publicar solo documentación/servicio autorizados con evidencia concreta. No limpiar volúmenes ni enviar secretos a logs/repo.

## Bibliografía / Referencias

[Fuentes compartidas](../soporte-semanas-4-5/FUENTES.md), D-01–04 y F-01–12. Docker Compose/redes y PostgreSQL oficial respaldan la forma propuesta, no su ejecución. Documento Boot consultado4.1.2 no actualiza la dependencia configurada4.1.1.

## Anexos

`despliegue/Dockerfile`, `compose.yaml`, `.env.example`, `.gitignore`, `context/.dockerignore`, instrucciones, estado de despliegue, trazabilidad/bitácora AA5 y nota individual. No se entrega un JAR antiguo, una imagen no construida como si existiese ni una URL de producción inventada.

## Declaración de uso de Inteligencia Artificial

Se utilizó ChatGPT/Codex para inspeccionar fuentes y preparar documentos/configuración. Las comprobaciones de artefactos son estáticas y no validan seguridad efectiva. La contribución/aceptación humanas y la ejecución de build/despliegue siguen pendientes; no se atribuyen a integrantes desconocidos.
