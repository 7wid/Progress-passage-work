package cn.edu.techgroup.outsourcing.modules.user.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

class PasswordChangedSessionListenerTest {

    @Test
    void keepsCurrentSessionAndDeletesOtherSessions() {
        @SuppressWarnings("unchecked")
        FindByIndexNameSessionRepository<Session> repository =
                mock(FindByIndexNameSessionRepository.class);
        Session current = mock(Session.class);
        Session other = mock(Session.class);
        when(current.getId()).thenReturn("current-session");
        when(other.getId()).thenReturn("other-session");
        when(repository.findByPrincipalName("requester")).thenReturn(Map.of(
                "current-session", current,
                "other-session", other));

        new PasswordChangedSessionListener(repository).handle(
                new PasswordChangedEvent("requester", "current-session"));

        verify(repository, never()).deleteById("current-session");
        verify(repository).deleteById("other-session");
    }
}
