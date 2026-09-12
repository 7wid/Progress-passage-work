package cn.edu.techgroup.outsourcing.modules.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.edu.techgroup.outsourcing.config.PasswordRecoveryProperties;
import cn.edu.techgroup.outsourcing.modules.auth.mapper.PasswordRecoveryMapper;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserStatus;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class RecoveryMailDeliveryTest {
    @Mock UserMapper users;
    @Mock PasswordRecoveryMapper mapper;
    @Mock ObjectProvider<JavaMailSender> provider;
    @Mock JavaMailSender sender;
    RecoveryMailDelivery delivery;

    @BeforeEach
    void setup() {
        delivery = new RecoveryMailDelivery(new PasswordRecoveryProperties(true, "security@example.org",
                "https://example.org/reset-password", Duration.ofMinutes(15)), users, mapper, provider);
    }

    @Test
    void unknownAndDisabledUsersReceiveNoMail() {
        delivery.deliver("unknown@example.org");
        UserEntity disabled = user();
        disabled.setStatus(UserStatus.DISABLED);
        when(users.selectByRecoveryEmail("user@example.org")).thenReturn(disabled);
        delivery.deliver("user@example.org");
        verifyNoInteractions(mapper, sender, provider);
    }

    @Test
    void sendsRandomFragmentLinkAndPersistsOnlyItsHash() {
        when(users.selectByRecoveryEmail("user@example.org")).thenReturn(user());
        when(provider.getObject()).thenReturn(sender);
        delivery.deliver("user@example.org");
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        ArgumentCaptor<PasswordRecoveryMapper.Token> saved = ArgumentCaptor.forClass(PasswordRecoveryMapper.Token.class);
        verify(sender).send(message.capture());
        verify(mapper).save(saved.capture());
        String token = message.getValue().getText().split("#token=")[1].split("\n")[0];
        assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
        assertEquals(RecoveryTokens.hash(token), saved.getValue().tokenHash());
        assertFalse(message.getValue().getText().contains("old-hash"));
        assertArrayEquals(new String[]{"user@example.org"}, message.getValue().getTo());
    }

    @Test
    void mailFailureInvalidatesOnlyTheTokenThatFailedToSend() {
        when(users.selectByRecoveryEmail("user@example.org")).thenReturn(user());
        when(provider.getObject()).thenReturn(sender);
        doThrow(new MailSendException("sensitive SMTP details")).when(sender).send(any(SimpleMailMessage.class));
        assertDoesNotThrow(() -> delivery.deliver("user@example.org"));
        ArgumentCaptor<PasswordRecoveryMapper.Token> saved = ArgumentCaptor.forClass(PasswordRecoveryMapper.Token.class);
        verify(mapper).save(saved.capture());
        verify(mapper).deleteToken(saved.getValue().tokenHash());
    }

    @Test
    void enabledFeatureWithoutMailSenderFailsStartup() {
        assertThrows(IllegalStateException.class, delivery::validateConfiguration);
    }

    @Test
    void remoteSmtpCannotDisableTransportEncryption() {
        var smtp = new org.springframework.mail.javamail.JavaMailSenderImpl();
        smtp.setHost("smtp.example.org");
        when(provider.getIfAvailable()).thenReturn(smtp);
        assertThrows(IllegalStateException.class, delivery::validateConfiguration);
        smtp.getJavaMailProperties().setProperty("mail.smtp.starttls.enable", "true");
        assertThrows(IllegalStateException.class, delivery::validateConfiguration);
        smtp.getJavaMailProperties().setProperty("mail.smtp.starttls.required", "true");
        assertDoesNotThrow(delivery::validateConfiguration);
    }

    @Test
    void completionNoticeDoesNotContainPasswordOrRecoveryLink() {
        when(provider.getObject()).thenReturn(sender);
        delivery.notifyReset("user@example.org");
        ArgumentCaptor<SimpleMailMessage> notice = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(notice.capture());
        assertTrue(notice.getValue().getText().contains("旧登录状态已失效"));
        assertFalse(notice.getValue().getText().contains("#token="));
    }

    private UserEntity user() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail("user@example.org");
        user.setPasswordHash("old-hash");
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
