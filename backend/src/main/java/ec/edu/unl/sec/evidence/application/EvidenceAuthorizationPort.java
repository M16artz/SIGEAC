package ec.edu.unl.sec.evidence.application;
import java.util.Optional;import java.util.UUID;
public interface EvidenceAuthorizationPort{Optional<Actor> currentActor();boolean canCollect(UUID actorId,UUID careerId,UUID processId,UUID indicatorId);boolean canDecideApplicability(UUID actorId,UUID careerId,UUID processId);record Actor(UUID identityId,boolean technical){}}
