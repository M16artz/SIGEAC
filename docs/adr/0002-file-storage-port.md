# ADR-0002: Puerto para almacenamiento externo de archivos

**Estado**: Aceptado
**Fecha**: 2026-10-18

## Contexto

Los binarios no pueden almacenarse en PostgreSQL y el desarrollo no puede depender de Docker o de un servicio S3.

## Decisión

El dominio usa un `FileStoragePort` con operaciones de preparar, promover, abrir y marcar para reconciliación. El MVP incluye adaptador local con claves opacas y raíz configurable; el contrato permite un adaptador S3 posterior. PostgreSQL conserva metadatos, estado y SHA-256.

## Consecuencias

- Desarrollo y pruebas funcionan con infraestructura local mínima.
- Las descargas siempre pasan por autorización del backend.
- Deben existir reconciliación, limpieza de temporales y pruebas de interrupción.
- El adaptador S3 no forma parte de la aceptación I0–I6.

## Alternativas descartadas

- `bytea`/large objects: contradice la constitución.
- S3 obligatorio: convertiría infraestructura externa en requisito del MVP.
