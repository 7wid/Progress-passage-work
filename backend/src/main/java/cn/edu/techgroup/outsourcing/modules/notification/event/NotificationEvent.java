package cn.edu.techgroup.outsourcing.modules.notification.event;

import cn.edu.techgroup.outsourcing.modules.notification.enums.NotificationType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record NotificationEvent(
        NotificationType type,
        Long requestId,
        Long actorId,
        List<Long> recipientIds,
        String title,
        String content) {

    public NotificationEvent {
        recipientIds = recipientIds == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(recipientIds));
    }
}
