package ec.edu.unl.sec.filestorage.domain;

public final class FileValidationException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public FileValidationException(String code) { super(code); }
}
