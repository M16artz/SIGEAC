package ec.edu.unl.sec.identityaccess.application;

import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import java.time.Instant;
import java.util.UUID;

public interface FunctionalAssignmentPolicyPort {
    Decision evaluate(UUID identityId,String functionCode,AuthorizationScope scope,Instant validFrom,Instant validTo);
    record Decision(boolean allowed,String reasonCode){public static Decision allow(){return new Decision(true,"ALLOWED");}public static Decision deny(String reason){return new Decision(false,reason);}}
}
