# ADR-0020: Sobre JSON común y controles del transporte HTTP

**Fecha:** 2026-11-08. **Estado:** implementado en el árbol de trabajo por solicitud
explícita del usuario; despliegue TLS/gateway y proveedor OIDC real pendientes de
configuración y comprobación. Esta decisión no se atribuye retrospectivamente al equipo.

## Contexto y problema

ACD5 exige `success/message/data/errors/meta`. La API devolvía DTO en éxitos,
Problem Details en errores y distintas formas de paginación. Los errores de
seguridad ocurren antes de los controladores. Cambiar solo los DTO dejaría
respuestas incompatibles y rompería consumidores.

## Alternativas

1. Modificar cada controlador y manejador: duplica transporte y facilita omisiones.
2. Envolver solo en el gateway: no corrige acceso directo al backend, pruebas ni Swagger.
3. Aplicar una política de transporte común y adaptar los clientes: centraliza la
   estructura y conserva los códigos HTTP, dominio, cabeceras y casos sin cuerpo.

## Decisión

Se adopta la tercera alternativa mediante `ApiResponseAdvice` y el escritor de
errores de seguridad. Los errores mantienen su detalle estructurado dentro de
`errors`, con media type exterior `application/json`; no se presenta el sobre
completo como un Problem Detail estándar. Los campos de paginación se trasladan
a `meta`. Las colecciones JSON simples tienen páginas de hasta 100 elementos.
204, 304, HEAD, binarios y documentos de protocolo conservan su formato.

Angular y los escenarios de aceptación adaptan el transporte a sus modelos de
vista. Los seis contratos se actualizan; el sobre base y los errores de transporte
compartidos tienen fuente canónica en el OpenAPI común.

Se añaden HTTPS obligatorio en `prod`, CORS de orígenes explícitos y límite
acotado por IP/instancia. Un gateway Nginx configura TLS también hacia el backend
y límites compartidos en ese gateway. El perfil local se liga a loopback.

OIDC/JWT es opcional, mediante Spring Security: firma, emisor, audiencia y
expiración verificadas; el `sub` debe corresponder a un UUID local activo. Los
roles externos no otorgan funciones locales. Las cookies siguen requiriendo CSRF.
ADR-0004 se conserva para las sesiones locales; esta decisión amplía las opciones
de autenticación sin afirmar que se haya conectado un proveedor institucional.

## Consecuencias y riesgos

- Es una **migración incompatible del contrato v1 previo al lanzamiento**. Se
  adaptan los consumidores del repositorio de forma coordinada. Un cliente externo
  debe migrar antes de desplegar; no se afirma compatibilidad binaria del JSON anterior.
  Para una API v1 ya publicada, exponer una nueva versión antes de aplicar este cambio.
- El límite en memoria protege una instancia; no sustituye límites de infraestructura
  ni defensa frente a DoS distribuido. Detrás del gateway se aplica además un límite
  agregado del backend, que no confía en `X-Forwarded-For` enviado por el cliente.
- Acotar una colección en la respuesta no convierte automáticamente su consulta
  interna en paginación SQL. Carreras y otras páginas existentes ya limitan en el
  repositorio; los catálogos que cargan listas deben revisarse si crece su volumen.
- Se requieren certificados reales, CA del backend y configuración OIDC del operador.
  No se incluyen claves privadas ni se desactiva la validación de certificados.
- Cerrar una sesión local no revoca un JWT. Su revocación corresponde al emisor;
  desactivar la identidad local sigue rechazando el acceso.

## Trazabilidad y verificación

Requisitos relacionados: FR-001/FR-002 de acceso, FR-043 de carreras y el contrato
de respuesta solicitado en ACD5. Arquitectura: monolito existente, filtros de
seguridad, capa HTTP común y gateway de entrada configurable.

Consultar [contrato y operación](../api/response-contract.qmd) y las pruebas
`ApiTransportIntegrationTest`, `ApiResponseAdviceTest`, `HttpBoundaryFilterTest`
y `OidcResourceServerConfigurationTest`. Los resultados actuales se registran
en [evidencia de la corrección](../../tests/acceptance/results/api-response-hardening.md).
