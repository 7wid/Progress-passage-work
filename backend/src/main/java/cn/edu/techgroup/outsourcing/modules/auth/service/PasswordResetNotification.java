package cn.edu.techgroup.outsourcing.modules.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PasswordResetNotification {
    public record Completed(String email) {}

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetNotification.class);
    private final TaskExecutor executor;
    private final RecoveryMailDelivery delivery;

    public PasswordResetNotification(@Qualifier("recoveryMailExecutor") TaskExecutor executor,
            RecoveryMailDelivery delivery) {
        this.executor = executor;
        this.delivery = delivery;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReset(Completed event) {
        try {
            executor.execute(() -> delivery.notifyReset(event.email()));
        } catch (TaskRejectedException exception) {
            LOGGER.error("Password reset notification queue is full");
        }
    }
}
