package cn.edu.techgroup.outsourcing.common.api;

import org.slf4j.MDC;

public record ApiResponse<T>(T data, String requestId) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, MDC.get("requestId"));
    }
}
