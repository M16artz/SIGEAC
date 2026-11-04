package ec.edu.unl.sec.identityaccess.domain;
import java.time.Instant;import java.util.*;
public record ConflictException(UUID id,UUID policyId,UUID identityId,AuthorizationScope scope,String justification,Instant validFrom,Instant validTo,UUID approvedBy,Instant revokedAt){public ConflictException{if(justification==null||justification.strip().length()<10||!validFrom.isBefore(validTo))throw new IllegalArgumentException("invalid exception");}public boolean activeAt(Instant n){return revokedAt==null&&!n.isBefore(validFrom)&&n.isBefore(validTo);}}
