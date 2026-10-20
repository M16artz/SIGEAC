package ec.edu.unl.sec.identityaccess.domain;
import java.util.*;
public record ProcessFunction(UUID id,String code,String name,Set<OperationCode> operations,boolean active){public ProcessFunction{Objects.requireNonNull(id);if(code==null||code.isBlank()||name==null||name.isBlank())throw new IllegalArgumentException("function fields required");operations=Set.copyOf(operations);}}
