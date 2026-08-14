package cn.edu.techgroup.outsourcing.modules.user.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MemberSessionInvalidationListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MemberSessionInvalidationListener.class);

    private final FindByIndexNameSessionRepository<? extends Session>
            sessionRepository;

    public MemberSessionInvalidationListener(
            FindByIndexNameSessionRepository<? extends Session>
                    sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MemberAccessChangedEvent event) {
        try {
            sessionRepository.findByPrincipalName(event.account())
                    .keySet()
                    .forEach(sessionRepository::deleteById);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Failed to invalidate sessions for changed member account={}",
                    event.account(),
                    exception);
        }
    }
}
