workspace "SEC-UNL" "Vistas acotadas al flujo documental implementado" {
    model {
        participant = person "Participante autorizado" "Responsable o revisor con ámbito vigente"
        sec = softwareSystem "SEC-UNL" "Autoevaluación de carreras; no concede acreditación" {
            web = container "Frontend" "Interfaz de usuario" "Angular"
            backend = container "Backend" "Autorizar y ejecutar casos de uso" "Spring Boot" {
                security = component "SessionAuthenticationFilter" "Autenticar sesión y comprobar CSRF" "Spring Security"
                api = component "EvidenceSourceCoverageController" "Recibir remisión y cobertura" "Spring MVC"
                submit = component "SubmitEvidenceService" "Validar y coordinar remisión" "Java"
                drafts = component "EvidenceDraftService" "Leer versión con autorización" "Java"
                repository = component "EvidenceGovernanceRepository" "Puerto de persistencia de remisión" "Java interface"
                audit = component "AuditPort" "Registrar evento transaccional" "Java interface"
            }
            db = container "Base de datos" "Dominio, metadatos y auditoría" "PostgreSQL"
            files = container "Almacenamiento de archivos" "Conservar binarios mediante FileStoragePort" "Sistema de archivos"
        }
        participant -> sec "Realizar autoevaluación"
        participant -> web "Utilizar interfaz"
        web -> backend "Solicitar operaciones" "REST /api/v1"
        backend -> db "Persistir y consultar" "JDBC/JPA"
        backend -> files "Guardar y recuperar binarios" "FileStoragePort"
        security -> api "Permitir solicitud autenticada"
        api -> submit "Remitir versión"
        submit -> drafts "Obtener versión autorizada"
        submit -> repository "Cambiar estado con revisión esperada"
        submit -> audit "Registrar EVIDENCE_SUBMITTED"
        repository -> db "Persistir mediante adaptador"
        audit -> db "Persistir mediante adaptador"
    }
    views {
        systemContext sec "C1" {
            include *
            autoLayout lr
        }
        container sec "C2" {
            include *
            autoLayout lr
        }
        component backend "C3" {
            include *
            autoLayout lr
        }
        styles {
            element "Person" {
                shape Person
            }
            element "Container" {
                background #246080
                color #ffffff
            }
            element "Component" {
                background #d8e8ef
            }
        }
    }
}
