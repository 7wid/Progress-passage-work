package cn.edu.techgroup.outsourcing.modules.audit.service;

public final class AuditActions {

    public static final String AUTH_LOGIN = "AUTH_LOGIN";
    public static final String AUTH_LOGIN_FAILED = "AUTH_LOGIN_FAILED";
    public static final String AUTH_LOGOUT = "AUTH_LOGOUT";
    public static final String REQUEST_SUBMITTED = "REQUEST_SUBMITTED";
    public static final String EVALUATION_CREATED = "EVALUATION_CREATED";
    public static final String EVALUATION_REJECTION_CONFIRMED =
            "EVALUATION_REJECTION_CONFIRMED";
    public static final String PROGRESS_RECORDED = "PROGRESS_RECORDED";
    public static final String DELIVERY_SUBMITTED = "DELIVERY_SUBMITTED";
    public static final String ACCEPTANCE_RECORDED = "ACCEPTANCE_RECORDED";

    private AuditActions() {}
}
