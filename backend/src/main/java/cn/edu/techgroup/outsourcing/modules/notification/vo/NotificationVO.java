package cn.edu.techgroup.outsourcing.modules.notification.vo;

import cn.edu.techgroup.outsourcing.modules.notification.entity.NotificationEntity;
import cn.edu.techgroup.outsourcing.modules.notification.enums.NotificationType;
import java.time.Instant;

public record NotificationVO(
        String id,
        NotificationType type,
        String title,
        String content,
        String requestId,
        boolean read,
        Instant readAt,
        Instant createdAt) {

    public static NotificationVO from(NotificationEntity entity) {
        return new NotificationVO(
                entity.getId().toString(),
                entity.getType(),
                entity.getTitle(),
                entity.getContent(),
                entity.getRequestId() == null
                        ? null
                        : entity.getRequestId().toString(),
                Boolean.TRUE.equals(entity.getRead()),
                entity.getReadAt(),
                entity.getCreatedAt());
    }
}
