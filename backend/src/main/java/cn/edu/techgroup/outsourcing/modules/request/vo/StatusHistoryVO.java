package cn.edu.techgroup.outsourcing.modules.request.vo;

import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import java.time.Instant;

public record StatusHistoryVO(
        String id,
        RequestStatus fromStatus,
        RequestStatus toStatus,
        String reason,
        String operatorName,
        Instant createdAt) {
}