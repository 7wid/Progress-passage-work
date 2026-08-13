package cn.edu.techgroup.outsourcing.modules.delivery.vo;

import java.time.Instant;

public record DeliveryVO(
        String id,
        String requestId,
        String submitterId,
        String submitterName,
        String description,
        String deliveryUrl,
        Instant createdAt) {
}
