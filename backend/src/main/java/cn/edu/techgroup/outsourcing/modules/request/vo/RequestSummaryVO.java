package cn.edu.techgroup.outsourcing.modules.request.vo;

import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestUrgency;
import java.time.Instant;
import java.time.LocalDate;

public record RequestSummaryVO(
        String id,
        String requestNo,
        String title,
        String categoryId,
        String categoryName,
        String creatorName,
        RequestUrgency urgency,
        RequestStatus status,
        int progress,
        LocalDate expectedDeadline,
        Instant submittedAt,
        Instant createdAt) {
}