package ec.edu.unl.sec.identityaccess.domain;
public record AuthorizationDecision(boolean allowed,String reasonCode){public static AuthorizationDecision allow(){return new AuthorizationDecision(true,"AUTHORIZED");}public static AuthorizationDecision deny(){return new AuthorizationDecision(false,"NOT_AUTHORIZED");}}
