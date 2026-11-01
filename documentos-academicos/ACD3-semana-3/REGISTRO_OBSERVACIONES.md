# ACD3 — Observaciones y ajustes

## 1. Hallazgos de preparación documental

Estos hallazgos surgen de la inspección asistida del repositorio. **No son observaciones recibidas del docente ni de otros equipos.** Las acciones son propuestas para decidir durante la clínica.

| ID | Observación y evidencia | Decisión propuesta | Ajuste documental preparado / pendiente |
|---|---|---|---|
| H-01 | DTO directos y Problem Details frente al sobre solicitado; controladores y handlers | Acordar migración compatible o excepción académica explícita | Separar contrato actual/objetivo en CONTRATO_API_INICIAL.md. No cambiar backend. |
| H-02 | Java 25 en research.md frente a java.version=21 en pom.xml | Confirmar versión de trabajo y corregir la fuente discrepante tras acuerdo | C2 muestra Java 21 configurado; DA-03 registra divergencia. |
| H-03 | PATCH de catálogo exige CareerInput completo | Acordar semántica/documento de modificación o mantener restricción explícita | Documentar entrada real; no presentarla como JSON Patch. |
| H-04 | POST state-decisions devuelve carrera con 201 y sin Location explícita | Aclarar si existe recurso de decisión consultable y su URI; o revisar estado/representación | No inventar GET de decisión ni Location inexistente. |
| H-05 | Proceso inicia revisión 0; catálogo en 1; ADR-0018 conserva compatibilidad | Mantener distinción salvo migración aprobada | Ejemplos y tabla conservan ambos valores. |
| H-06 | Modelo publicado permite preparar; aplicabilidad se exige después en activación | Confirmar límites de Unidad 1 y validación normativa | F-ACD3-02 evita equiparar PREPARATION con autorización/aplicabilidad. |
| H-07 | AA2 HTTP-AA-01: desafío 401 no fijado explícitamente en escritor revisado | Comprobar intercambio real y decidir solución compatible con cookie | No inventar Bearer ni declarar conformidad HTTP completa. |
| H-08 | `/processes` devuelve lista sin parámetros de página | Mantener contrato real o aprobar diseño paginado | No fabricar meta.page/total en contrato actual. |
| H-09 | APE3 comprobó 409 MODEL_VERSION_REQUIRES_V2 al consultar un modelo configurado con el DTO legado y 200 en /configured | Explicitar compatibilidad de representación en OP-14 | Corregidos contrato y LaTeX; evidencia posterior en APE3 HTTP-16/16B, sin atribución docente. |

## 2. Preguntas para resolver en clínica

1. ¿Se ratifica el recorte de catálogo y preparación, excluyendo activación/gobierno?
2. ¿Quién puede administrar catálogo y gestionar procesos en cada ámbito? Confirmar asignaciones sin equiparar permisos técnicos con competencias institucionales.
3. ¿Cómo se cumplirá el sobre de la guía sin romper los clientes v1 y el objetivo v2 existente?
4. ¿Se conserva PATCH con datos editables completos o se especifica un documento parcial distinto?
5. ¿Qué recurso crea state-decisions y qué URI/estado deben representar la operación?
6. ¿Cuál es la versión Java acordada para código, documentación y herramientas?
7. ¿Qué entorno/orígenes se usarán para probar cookies, TLS, CORS y cabeceras?
8. ¿Qué alcance y casos de prueba demostrarán atomicidad, visibilidad y revisión concurrente?

## 3. Registro de retroalimentación auténtica

Estado inicial: **sin registro de sesión aportado**. Completar exclusivamente con observaciones recibidas realmente.

| ID | Fecha / persona y rol | Observación recibida | Decisión y justificación | Cambio realizado o razón para mantener | Requisito / archivo / evidencia | Responsable |
|---|---|---|---|---|---|---|
| OBS-01 | Pendiente | Pendiente de sesión | Pendiente | Pendiente | Pendiente de enlace/commit real | Pendiente |

No convertir H-01–09 en OBS sin una observación real identificada. Si se mantiene el diseño, registrar el argumento y la fuente, no inventar un cambio para llenar la tabla.

## 4. Cambios ya preparados en esta actividad

Se elaboraron dos procesos textuales, tres vistas C4, ocho decisiones con alternativas, selección de 18 operaciones, contrato objetivo diferenciado y trazabilidad de Semana 2 a componentes/API. Son cambios en documentación académica; no hubo implementación, migración de contratos, ejecución de negocio, publicación ni commit nuevo. Las correcciones derivadas de la clínica permanecen pendientes hasta recibir retroalimentación real.
