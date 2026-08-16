package cn.edu.techgroup.outsourcing.modules.user.event;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PasswordChangedSessionListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PasswordChangedSessionListener.class);

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    public PasswordChangedSessionListener(
            FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PasswordChangedEvent event) {
        try {
            sessionRepository.findByPrincipalName(event.account())
                    .forEach((sessionId, session) -> {
                        if (!Objects.equals(sessionId, event.currentSessionId())
                                && !Objects.equals(session.getId(), event.currentSessionId())) {
                            sessionRepository.deleteById(sessionId);
                        }
                    });
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Failed to invalidate old sessions after password change for account={}",
                    event.account(),
                    exception);
        }
    }
}
