package ec.edu.unl.sec.identityaccess.domain;
import java.time.Instant;import java.util.*;
public record ConflictPolicy(UUID id,UUID leftFunctionId,UUID rightFunctionId,Instant validFrom,Instant validTo,boolean active){public ConflictPolicy{if(leftFunctionId.equals(rightFunctionId)||!validFrom.isBefore(validTo))throw new IllegalArgumentException("invalid conflict policy");}public boolean activeAt(Instant n){return active&&!n.isBefore(validFrom)&&n.isBefore(validTo);}}
