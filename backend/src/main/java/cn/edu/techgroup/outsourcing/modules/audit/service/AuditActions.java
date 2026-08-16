package cn.edu.techgroup.outsourcing.modules.audit.service;

public final class AuditActions {

    public static final String AUTH_LOGIN = "AUTH_LOGIN";
    public static final String AUTH_LOGIN_FAILED = "AUTH_LOGIN_FAILED";
    public static final String AUTH_LOGOUT = "AUTH_LOGOUT";
    public static final String PROFILE_UPDATED = "PROFILE_UPDATED";
    public static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";
    public static final String USER_REGISTERED = "USER_REGISTERED";
    public static final String REQUEST_DRAFT_SAVED = "REQUEST_DRAFT_SAVED";
    public static final String REQUEST_CONTENT_UPDATED = "REQUEST_CONTENT_UPDATED";
    public static final String REQUEST_SUBMITTED = "REQUEST_SUBMITTED";
    public static final String REQUEST_CANCELLED_BY_REQUESTER =
            "REQUEST_CANCELLED_BY_REQUESTER";
    public static final String EVALUATION_CREATED = "EVALUATION_CREATED";
    public static final String EVALUATION_REJECTION_CONFIRMED =
            "EVALUATION_REJECTION_CONFIRMED";
    public static final String PROGRESS_RECORDED = "PROGRESS_RECORDED";
    public static final String DELIVERY_SUBMITTED = "DELIVERY_SUBMITTED";
    public static final String ACCEPTANCE_RECORDED = "ACCEPTANCE_RECORDED";

    private AuditActions() {}
}
