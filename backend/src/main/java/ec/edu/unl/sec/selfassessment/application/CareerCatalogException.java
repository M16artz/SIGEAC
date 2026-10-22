package ec.edu.unl.sec.selfassessment.application;
public class CareerCatalogException extends RuntimeException {
    private final int status; private final String code;
    public CareerCatalogException(int status,String code,String message) {super(message);this.status=status;this.code=code;}
    public int status(){return status;} public String code(){return code;}
}
