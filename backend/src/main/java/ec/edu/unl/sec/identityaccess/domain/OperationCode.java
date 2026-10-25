package ec.edu.unl.sec.identityaccess.domain;

public record OperationCode(String value) {
    public static final OperationCode CAREER_ADMIN = new OperationCode("CAREER_ADMIN");
    public static final OperationCode CAREER_READ = new OperationCode("CAREER_READ");
    public static final OperationCode ACCESS_READ_SELF = new OperationCode("ACCESS_READ_SELF");
    public static final OperationCode ACCESS_MANAGE_USERS = new OperationCode("ACCESS_MANAGE_USERS");
    public static final OperationCode ACCESS_ASSIGN_SCOPE = new OperationCode("ACCESS_ASSIGN_SCOPE");
    public static final OperationCode ACCESS_MANAGE_EXCEPTIONS = new OperationCode("ACCESS_MANAGE_EXCEPTIONS");
    public static final OperationCode ACCESS_READ_AUDIT = new OperationCode("ACCESS_READ_AUDIT");
    public static final OperationCode EVIDENCE_COLLECT = new OperationCode("EVIDENCE_COLLECT");
    public static final OperationCode EVIDENCE_REVIEW = new OperationCode("EVIDENCE_REVIEW");
    public static final OperationCode INDICATOR_EVALUATE = new OperationCode("INDICATOR_EVALUATE");
    public static final OperationCode PROCESS_COORDINATE = new OperationCode("PROCESS_COORDINATE");
    public static final OperationCode PROGRESS_READ = new OperationCode("PROGRESS_READ");
    public static final OperationCode DOCUMENTARY_SNAPSHOT_CREATE = new OperationCode("DOCUMENTARY_SNAPSHOT_CREATE");
    public static final OperationCode PROCESS_DOCUMENTARY_CLOSE = new OperationCode("PROCESS_DOCUMENTARY_CLOSE");
    public static final OperationCode PROCESS_INTEGRAL_CLOSE = new OperationCode("PROCESS_INTEGRAL_CLOSE");
    public static final OperationCode REPORT_REVIEW = new OperationCode("REPORT_REVIEW");
    public static final OperationCode REPORT_APPROVE = new OperationCode("REPORT_APPROVE");
    public static final OperationCode IMPROVEMENT_PLAN_CREATE = new OperationCode("IMPROVEMENT_PLAN_CREATE");
    public static final OperationCode IMPROVEMENT_ACTION_EXECUTE = new OperationCode("IMPROVEMENT_ACTION_EXECUTE");
    public static final OperationCode IMPROVEMENT_ACTION_VERIFY = new OperationCode("IMPROVEMENT_ACTION_VERIFY");
    public static final OperationCode ARCHIVE_READ = new OperationCode("ARCHIVE_READ");
    public static final OperationCode ARCHIVE_CLOSE = new OperationCode("ARCHIVE_CLOSE");
    public static final OperationCode PROCESS_MANAGE = new OperationCode("PROCESS_MANAGE");
    public static final OperationCode PROCESS_AUTHORIZE = new OperationCode("PROCESS_AUTHORIZE");
    public static final OperationCode MODEL_MANAGE = new OperationCode("MODEL_MANAGE");
    public static final OperationCode MODEL_READ = new OperationCode("MODEL_READ");
    public static final OperationCode MODEL_PUBLISH = new OperationCode("MODEL_PUBLISH");
    public static final OperationCode MODEL_AUDIT_READ = new OperationCode("MODEL_AUDIT_READ");
    public static final OperationCode MODEL_APPLICABILITY_AUTHORIZE = new OperationCode("MODEL_APPLICABILITY_AUTHORIZE");

    public OperationCode {
        if (value == null || !value.matches("[A-Z][A-Z0-9_.-]{2,149}")) {
            throw new IllegalArgumentException("invalid operation code");
        }
    }
}
