package ec.edu.unl.sec.identityaccess.application;
import java.util.*;
public interface CurrentActorPort{Optional<Actor> currentActor();record Actor(UUID identityId,boolean technical){}}
