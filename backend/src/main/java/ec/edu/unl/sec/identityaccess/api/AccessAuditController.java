package ec.edu.unl.sec.identityaccess.api;

import ec.edu.unl.sec.audit.application.AuditQueryPort;
import ec.edu.unl.sec.identityaccess.application.IdentityAdministrationService;
import ec.edu.unl.sec.identityaccess.domain.IdentityAccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class AccessAuditController {
    private final IdentityAdministrationService administration;
    private final AuditQueryPort auditQuery;

    public AccessAuditController(IdentityAdministrationService administration, AuditQueryPort auditQuery) {
        this.administration = administration;
        this.auditQuery = auditQuery;
    }

    @GetMapping("/audit-events")
    public IdentityAdministrationController.PageView<AuditQueryPort.AuditEventView> search(
            Authentication authentication,
            HttpServletRequest request,
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) UUID objectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID identityId = AccessRequestContext.identityId(authentication);
        if (identityId == null) throw new IdentityAccessDeniedException("AUTHENTICATION_REQUIRED");
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        var actor = new IdentityAdministrationService.Actor(
                identityId, AccessRequestContext.correlation(request));
        List<UUID> careerIds = administration.auditCareerIds(actor);
        List<AuditQueryPort.AuditEventView> items =
                auditQuery.search(careerIds, true, objectType, objectId, safePage * safeSize, safeSize);
        long total = auditQuery.count(careerIds, true, objectType, objectId);
        return new IdentityAdministrationController.PageView<>(items, safePage, safeSize, total);
    }
}
