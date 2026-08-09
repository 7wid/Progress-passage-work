package cn.edu.techgroup.outsourcing.modules.request.vo;

import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestUrgency;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RequestDetailVO(
        String id,
        String requestNo,
        String title,
        String categoryId,
        String categoryName,
        String creatorId,
        String creatorName,
        String background,
        String description,
        String expectedResult,
        LocalDate expectedDeadline,
        RequestUrgency urgency,
        BigDecimal budgetAmount,
        String budgetDescription,
        String technicalConstraints,
        String contactInfo,
        RequestStatus status,
        int progress,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt,
        List<StatusHistoryVO> statusHistory) {
}