package ec.edu.unl.sec.shared.api;

import org.springframework.http.HttpStatus;

public class DomainProblemException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public DomainProblemException(HttpStatus status, String code, String safeMessage) {
        super(safeMessage);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
