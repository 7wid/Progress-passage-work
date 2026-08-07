package cn.edu.techgroup.outsourcing.common.error;

import java.util.List;
import org.slf4j.MDC;

public record ErrorResponse(ErrorBody error, String requestId) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorBody(code, message, List.of()), MDC.get("requestId"));
    }

    public static ErrorResponse of(String code, String message, List<FieldErrorDetail> details) {
        return new ErrorResponse(new ErrorBody(code, message, details), MDC.get("requestId"));
    }

    public record ErrorBody(String code, String message, List<FieldErrorDetail> details) {}

    public record FieldErrorDetail(String field, String message) {}
}
