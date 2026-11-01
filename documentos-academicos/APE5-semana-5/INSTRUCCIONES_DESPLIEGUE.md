# APE5 — Construcción y despliegue local propuestos

**Estado:** Dockerfile y Compose preparados, sin construcción, arranque, publicación ni URL desplegada. No hay Docker/Podman en PATH ni sandbox completo validado; no se ejecutó código del proyecto. Estos archivos sirven al backend Spring existente, no implementan un despliegue DRF. El tag local `unverified` evita etiquetar como validado un artefacto sin prueba.

## Prerrequisitos y límites

Resolver la tecnología y el contrato pendientes de APE3. Si se acepta Spring, preparar JDK/Maven y dependencias ya disponibles y verificar el código en sandbox conforme; no usar el JAR antiguo de target sin reconstruir y registrar hash/commit. Confirmar motor Linux de contenedores, versiones y recursos. Las imágenes Java 25 JRE y PostgreSQL 18 deben estar auditadas, fijadas por digest y disponibles localmente; no se descargaron aquí. El Dockerfile espera una imagen Linux con java, mkdir y chown: verificar esa interfaz, no asumir compatibilidad de una distroless arbitraria.

La red Compose interna y los límites son controles de operación propuestos, **no prueba suficiente de la sandbox de auditoría**. No montar Docker socket, repositorio completo, home o credenciales del host. El contexto de construcción contiene solo app.jar mediante allowlist; `.env` queda fuera y está ignorado. No imprimir `docker compose config` con secretos interpolados ni adjuntar `docker inspect` sin redacción.

## Preparación reproducible prevista

1. En el entorno de construcción aislado, ejecutar Maven verify/package del commit seleccionado y conservar resultados y SHA-256 del JAR. Comprobar que sus dependencias/Java corresponden al POM y contrato; esta entrega no ejecuta ese paso.
2. Copiar **solo** el JAR nuevo a `despliegue/context/app.jar`. No versionarlo. Comparar hash con el artefacto verificado antes de construir.
3. Crear `despliegue/.env` local desde `.env.example`; proporcionar imágenes por digest, contraseña sintética de DB y secreto HMAC nuevo de alta entropía. No usar cadenas de ejemplo ni publicar valores. El archivo de ejemplo no contiene claves reales.
4. Revisar Compose y validar sin revelar valores. En la máquina autorizada, desde `despliegue`, la forma prevista es:

```text
docker compose config --quiet
docker compose build --pull=false api
docker compose up -d --no-build
docker compose ps
```

Son instrucciones **no ejecutadas**. Requieren validación semántica con la versión instalada y las imágenes elegidas. El contexto restringido evita enviar código/.git/secretos innecesarios al builder. La política pull never y build pull=false no reemplaza aislar el builder de red externa.

## Persistencia, arranque y seguridad

PostgreSQL 18 oficial usa almacenamiento bajo `/var/lib/postgresql`, diferente de convenciones de versiones previas: el volumen se fija ahí y debe contrastarse con la imagen/digest. Nunca sustituir una imagen mayor sobre un volumen existente sin procedimiento de migración/restauración. La DB no publica puerto al host. API escucha en contenedor y expone **solo loopback** 18095; es una dirección prevista, no URL desplegada.

Flyway inicializa/actualiza esquema mediante configuración del producto. La propuesta usa `sec_owner` para migración y arranque por simplicidad del fixture; esto no acredita mínimo privilegio para producción. Separar cuenta migradora y cuenta de ejecución y probar permisos antes de un despliegue real. Mantener backup y ensayar restauración coherente de DB y archivos; no se ejecutaron backup/restore aquí.

La API usa prod, demo=false y cookie Secure=true, sin Swagger activo. Un navegador no debe enviar esa cookie por el HTTP local previsto: para demostrar sesión productiva se requiere TLS/proxy configurado y verificado, o un fixture local diferente claramente separado. **No desactivar Secure en prod para hacer pasar una demo.** No se declara TLS, CORS ni certificados desplegados.

La imagen ejecuta como UID 10001 y raíz de contenedor de solo lectura; `/tmp` y volumen de archivos son escrituras explícitas. Revisar permisos/propietarios reales de volúmenes e imagen; no afirmar aislamiento por copiar estas líneas. Configuración por env queda visible a operadores del motor: evaluar secretos montados/gestor externo si el entorno lo requiere.

## Validación y documentación de API

Registrar estado de salud, logs redactados de arranque, migraciones, digest imagen, hash JAR, versión DB y resultados del flujo protegido. `pg_isready` solo indica disponibilidad de DB, no permisos ni éxito de API. Una respuesta health 200 no demuestra funcionalidad del incremento. Ejecutar casos VT y contrato aceptado en el aislamiento requerido, incluidos rechazos, reinicio y persistencia.

El perfil prod deshabilita `/swagger-ui` y `/v3/api-docs` por diseño. Publicar **documentación estática del contrato aceptado** o un servicio de documentación separado y controlado; no habilitar demo/Swagger productivo automáticamente. Los YAML de APE3 están disponibles localmente; el objetivo sigue no implementado. No se publicó documentación en un host externo.

## Parada, datos y rollback

Usar parada del proyecto específico; no comandos globales de limpieza ni borrado de volúmenes. Guardar respaldo antes de cambios; seleccionar imagen previa y plan de compatibilidad de schema. Un rollback de imagen no revierte una migración de DB. No se proporciona un `down -v` como operación rutinaria.

## Evidencia aún necesaria

Resultado real de build, parser Compose, arranque, flujo protegido con DB principal, pruebas de persistencia/restore, TLS/configuración, URL o captura/log local de despliegue, commit y contribución por integrante. El archivo `evidencias/estado-despliegue.json` registra que todo ello sigue pendiente; no constituye despliegue.
