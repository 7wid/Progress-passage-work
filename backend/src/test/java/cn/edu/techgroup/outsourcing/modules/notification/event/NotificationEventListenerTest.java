package cn.edu.techgroup.outsourcing.modules.notification.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import cn.edu.techgroup.outsourcing.modules.notification.enums.NotificationType;
import cn.edu.techgroup.outsourcing.modules.notification.service.NotificationService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationEventListener(notificationService);
    }

    @Test
    void dispatchesCommittedEvent() {
        NotificationEvent event = event();

        listener.handle(event);

        verify(notificationService).dispatch(event);
    }

    @Test
    void notificationFailureDoesNotEscapeToCoreBusiness() {
        NotificationEvent event = event();
        doThrow(new IllegalStateException("database unavailable"))
                .when(notificationService)
                .dispatch(event);

        assertDoesNotThrow(() -> listener.handle(event));
    }

    private NotificationEvent event() {
        return new NotificationEvent(
                NotificationType.DELIVERY_SUBMITTED,
                100L,
                2L,
                List.of(1L),
                "等待验收",
                "需求 TR-100 已提交交付。" );
    }
}
