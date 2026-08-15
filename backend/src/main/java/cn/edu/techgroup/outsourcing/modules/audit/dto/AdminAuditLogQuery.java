package cn.edu.techgroup.outsourcing.modules.audit.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record AdminAuditLogQuery(
        @Min(value = 1, message = "页码不能小于 1") Integer page,
        @Min(value = 1, message = "每页数量不能小于 1")
        @Max(value = 100, message = "每页数量不能超过 100") Integer pageSize,
        @Positive(message = "操作者 ID 必须为正数") Long actorId,
        @Size(max = 80, message = "动作代码不能超过 80 个字符") String action,
        @Size(max = 80, message = "目标类型不能超过 80 个字符") String targetType,
        @Size(max = 80, message = "目标 ID 不能超过 80 个字符") String targetId,
        @Size(max = 80, message = "请求 ID 不能超过 80 个字符") String requestId,
        @PastOrPresent(message = "开始日期不能晚于今天")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @PastOrPresent(message = "结束日期不能晚于今天")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

    public AdminAuditLogQuery {
        page = page == null ? 1 : page;
        pageSize = pageSize == null ? 20 : pageSize;
        action = trimToNull(action);
        targetType = trimToNull(targetType);
        targetId = trimToNull(targetId);
        requestId = trimToNull(requestId);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
