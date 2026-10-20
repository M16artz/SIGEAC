package ec.edu.unl.sec.identityaccess.domain;
import java.time.Instant;import java.util.*;
public record Credential(UUID id,UUID identityId,String bcryptHash,boolean active,Instant createdAt){public Credential{Objects.requireNonNull(id);Objects.requireNonNull(identityId);if(bcryptHash==null||!bcryptHash.startsWith("$2"))throw new IllegalArgumentException("BCrypt hash required");Objects.requireNonNull(createdAt);}}
