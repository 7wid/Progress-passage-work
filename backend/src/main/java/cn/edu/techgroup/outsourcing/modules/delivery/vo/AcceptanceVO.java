package cn.edu.techgroup.outsourcing.modules.delivery.vo;

import cn.edu.techgroup.outsourcing.modules.delivery.enums.AcceptanceResult;
import java.time.Instant;

public record AcceptanceVO(
        String id,
        String requestId,
        String deliveryId,
        String operatorId,
        String operatorName,
        AcceptanceResult result,
        String comment,
        Instant createdAt) {
}
