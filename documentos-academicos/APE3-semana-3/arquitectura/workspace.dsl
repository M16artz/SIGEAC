workspace "SIGEAC APE3" "Catalogo y preparacion de procesos; arquitectura observada, no migracion Django aprobada" {
    !identifiers hierarchical
    model {
        admin = person "Administrador funcional" "Mantener catalogo mediante capacidades autorizadas"
        gestor = person "Gestor autorizado" "Preparar procesos en su ambito"
        lector = person "Lector autorizado" "Consultar informacion visible"
        sigeac = softwareSystem "SIGEAC / SEC-UNL" "Organizar autoevaluacion de carreras con referencias y trazabilidad" {
            web = container "Aplicacion web" "Capturar entradas y presentar resultados" "Angular 22.2.3 / TypeScript"
            api = container "Backend API" "Autorizar y ejecutar casos de uso; monolito modular existente" "Java 21 configurado / Spring Boot 4.1.1" {
                acceso = component "Identidad y acceso" "SessionAuthenticationFilter, SessionService y ScopeAuthorizationService" "Spring Security / sesiones opacas"
                http = component "API de catalogo y procesos" "CareerController y SelfAssessmentController; traducir HTTP" "Spring MVC"
                catalogo = component "Aplicacion de catalogo" "CareerAdministrationService; validar permisos, unidad, unicidad y revision" "Servicio transaccional Java"
                procesos = component "Aplicacion de procesos" "SelfAssessmentService; preparar y consultar sin activar" "Servicio transaccional Java"
                dominio = component "Dominio" "Career, AcademicUnit y SelfAssessmentProcess; invariantes" "Records y reglas Java"
                modelos = component "Catalogo de modelos" "ModelCatalogPort y ModelCatalogAdapter; resolver version publicada" "Puerto y adaptador interno"
                repos = component "Persistencia" "CareerCatalogRepository y SelfAssessmentRepository con adaptadores JDBC" "JDBC / PostgreSQL"
                audit = component "Auditoria" "AuditPort y SelfAssessmentAuditService; registrar cambio critico" "Puerto y servicio Java"
            }
            db = container "Base de datos" "Catalogo, modelos, procesos, identidad y auditoria" "PostgreSQL" {
                tags "Database"
            }
        }
        admin -> sigeac "Mantener catalogo autorizado"
        gestor -> sigeac "Preparar y consultar procesos"
        lector -> sigeac "Consultar datos visibles"
        admin -> sigeac.web "Usar formularios"
        gestor -> sigeac.web "Preparar proceso"
        lector -> sigeac.web "Consultar"
        sigeac.web -> sigeac.api "Invocar API con cookie y CSRF" "HTTP JSON; TLS segun despliegue"
        sigeac.api -> sigeac.db "Persistir y consultar" "SQL / JDBC"
        sigeac.web -> sigeac.api.acceso "Autenticar solicitud" "HTTP"
        sigeac.api.acceso -> sigeac.api.http "Continuar cadena de filtros"
        sigeac.api.http -> sigeac.api.catalogo "Invocar caso de catalogo"
        sigeac.api.http -> sigeac.api.procesos "Invocar caso de proceso"
        sigeac.api.catalogo -> sigeac.api.acceso "Consultar actor y permisos por puertos"
        sigeac.api.procesos -> sigeac.api.acceso "Consultar actor y ambito por puertos"
        sigeac.api.procesos -> sigeac.api.catalogo "Comprobar carrera vigente"
        sigeac.api.procesos -> sigeac.api.modelos "Resolver version publicada"
        sigeac.api.catalogo -> sigeac.api.dominio "Construir y validar"
        sigeac.api.procesos -> sigeac.api.dominio "Construir y validar"
        sigeac.api.catalogo -> sigeac.api.repos "Conservar catalogo e historial"
        sigeac.api.procesos -> sigeac.api.repos "Conservar proceso"
        sigeac.api.catalogo -> sigeac.api.audit "Registrar cambio"
        sigeac.api.procesos -> sigeac.api.audit "Registrar cambio"
        sigeac.api.repos -> sigeac.db "Ejecutar SQL"
        sigeac.api.modelos -> sigeac.db "Consultar mediante infraestructura de modelos"
        sigeac.api.audit -> sigeac.db "Persistir mediante infraestructura de auditoria"
    }
    views {
        systemContext sigeac "C1" {
            include admin gestor lector sigeac
            autolayout lr
        }
        container sigeac "C2" {
            include admin gestor lector sigeac.web sigeac.api sigeac.db
            autolayout lr
        }
        component sigeac.api "C3" {
            include sigeac.web sigeac.db sigeac.api.acceso sigeac.api.http sigeac.api.catalogo sigeac.api.procesos sigeac.api.dominio sigeac.api.modelos sigeac.api.repos sigeac.api.audit
            autolayout lr
        }
        styles {
            element "Person" {
                shape Person
                background #08427b
                color #ffffff
            }
            element "Software System" {
                background #1168bd
                color #ffffff
            }
            element "Container" {
                background #438dd5
                color #ffffff
            }
            element "Database" {
                shape Cylinder
            }
            element "Component" {
                background #85bbf0
                color #000000
            }
        }
    }
}
