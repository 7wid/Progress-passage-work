package cn.edu.techgroup.outsourcing.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "请求参数不正确"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED,"账号或密码错误"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "请先登录"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "没有权限执行此操作"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "目标数据不存在"),
    DATA_VERSION_CONFLICT(HttpStatus.CONFLICT, "数据已被其他操作更新，请刷新后重试"),
    REQUEST_STATUS_CONFLICT(HttpStatus.CONFLICT, "当前需求状态不允许此操作"),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "数据已存在"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "操作过于频繁，请稍后重试"),
    RECOVERY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "密码找回暂时不可用，请稍后重试或联系管理员"),
    INVALID_RESET_TOKEN(HttpStatus.BAD_REQUEST, "重置链接无效或已过期，请重新申请"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "服务器暂时无法处理请求");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
