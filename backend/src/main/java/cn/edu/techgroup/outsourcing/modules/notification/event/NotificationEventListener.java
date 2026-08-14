package cn.edu.techgroup.outsourcing.modules.notification.event;

import cn.edu.techgroup.outsourcing.modules.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationEventListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NotificationEvent event) {
        try {
            notificationService.dispatch(event);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Failed to dispatch notification: type={}, requestId={}",
                    event.type(),
                    event.requestId(),
                    exception);
        }
    }
}
