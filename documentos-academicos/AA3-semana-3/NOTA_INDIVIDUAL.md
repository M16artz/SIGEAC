# AA3 — Nota individual y cambios respecto de Semana 3

**Estudiante / equipo / fecha real:** pendientes. Duración de guía: 4,5 horas; no se registra como tiempo personal ya trabajado.

## Aporte preparado para revisión

Se investigó dónde construir el contrato de salida, una subdecisión abierta de APE3-002. Se compararon tres opciones con siete criterios y se redactó ADR-AA3-001 en estado Propuesto, con alternativa C seleccionada condicionalmente. Se enlazaron requisitos/AC, C3, cliente, handlers/writer, siete riesgos y cinco verificaciones futuras.

La asistencia de ChatGPT/Codex produjo este material. No demuestra que un estudiante concreto haya realizado la lectura, comparación, defensa oral o aceptación; se debe registrar el trabajo personal realmente hecho.

## Diferencias frente a la versión previa

| Antes, en ACD3/APE3 | Refinamiento AA3 | Motivo y fuente |
|---|---|---|
| APE3-002 enumera opciones de compatibilidad del sobre | Separar aceptación del sobre de la decisión sobre responsabilidad interna; elegir C como propuesta condicionada | Evitar que una alternativa técnica se confunda con aprobación docente. P-03/09. |
| No se concreta cómo cubrir writer previo al controlador | Comparar alcance MVC y salida de AccessProblemWriter | P-06 y S-03 muestran caminos distintos; PF-AA3-02 pendiente. |
| Se reconoce ruptura potencial del cliente | Identificar rememberCsrf, isApiProblem y result.items | P-07 aporta consumidores concretos, sin afirmar ejecución nueva. |
| Existen variantes de paginación en propuestas | Registrar elección pendiente entre meta y CatalogPage dentro de data | P-02/05; no ocultar discrepancia de diseño. |
| Hay tests del contrato actual | Separar evidencia P-08 de PF-AA3-01–05 | Evitar declarar probado un sobre/mapeador inexistente. |

## Evidencia personal que debe completar el estudiante

- Requisito/criterio que revisó: **pendiente**.
- Alternativas que contrastó y por qué eligió una: **pendiente**.
- Fuente oficial leída y conclusión propia sustentada: **pendiente de URL/localizador**.
- Archivo/símbolo del proyecto contrastado: **pendiente**.
- Corrección realizada personalmente y justificación: **pendiente**.
- Punto con el que discrepa o decisión que mantiene abierta: **pendiente**.
- Evidencia real de contribución: **pendiente de commit/PR/historial y vínculo**.

No se inventa retroalimentación, validación humana ni un commit nuevo. El commit base del manifiesto identifica el repositorio analizado y no el aporte individual a AA3. Una vez revisado, versionar con el flujo acordado por el equipo y sin incluir archivos ajenos indiscriminadamente.
