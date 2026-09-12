package cn.edu.techgroup.outsourcing.modules.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.config.PasswordRecoveryProperties;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.auth.dto.ResetPasswordCommand;
import cn.edu.techgroup.outsourcing.modules.auth.mapper.PasswordRecoveryMapper;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserStatus;
import cn.edu.techgroup.outsourcing.modules.user.event.PasswordChangedEvent;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTest {
    @Mock RecoveryRateLimiter limiter;
    @Mock TaskExecutor executor;
    @Mock RecoveryMailDelivery delivery;
    @Mock PasswordRecoveryMapper mapper;
    @Mock UserMapper users;
    @Mock PasswordEncoder encoder;
    @Mock AuditRecorder audit;
    @Mock ApplicationEventPublisher events;
    PasswordRecoveryService service;
    final String token = "a".repeat(43);

    @BeforeEach
    void setup() {
        service = new PasswordRecoveryService(new PasswordRecoveryProperties(true,
                "security@example.org", "https://example.org/reset-password", Duration.ofMinutes(15)),
                limiter, executor, delivery, mapper, users, encoder, audit, events);
    }

    @Test
    void requestQueuesWithoutLookingUpAccountOnHttpThread() {
        when(limiter.allow(anyString(), anyString(), anyInt(), anyInt())).thenReturn(true);
        when(limiter.allowEmailCooldown(anyString())).thenReturn(true);
        service.request("user@example.org", "192.0.2.1");
        verify(executor).execute(any());
        verifyNoInteractions(users, delivery, mapper);
    }

    @Test
    void emailThrottlingReturnsNormallyWithoutSending() {
        when(limiter.allow("send-global", "all", 500, 3600)).thenReturn(true);
        when(limiter.allow("send-ip", "192.0.2.1", 20, 900)).thenReturn(true);
        assertDoesNotThrow(() -> service.request("user@example.org", "192.0.2.1"));
        verifyNoInteractions(executor, users);
    }

    @Test
    void ipThrottlingRejectsAndDoesNotEnqueue() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.request("user@example.org", "192.0.2.1"));
        assertEquals(ErrorCode.TOO_MANY_REQUESTS, exception.getErrorCode());
        verifyNoInteractions(executor, users);
    }

    @Test
    void saturatedExecutorReturnsServiceUnavailable() {
        when(limiter.allow(anyString(), anyString(), anyInt(), anyInt())).thenReturn(true);
        when(limiter.allowEmailCooldown(anyString())).thenReturn(true);
        doThrow(new TaskRejectedException("full")).when(executor).execute(any());
        assertEquals(ErrorCode.RECOVERY_UNAVAILABLE, assertThrows(BusinessException.class,
                () -> service.request("user@example.org", "192.0.2.1")).getErrorCode());
    }

    @Test
    void expiredOrReplayedTokenCannotChangePassword() {
        var expired = new PasswordRecoveryMapper.Token(1L, RecoveryTokens.hash(token), "user@example.org",
                "fingerprint", Instant.now().minusSeconds(1));
        when(mapper.findToken(anyString())).thenReturn(null, expired);
        when(mapper.lockToken(anyString())).thenReturn(expired);
        for (int i = 0; i < 2; i++) {
            assertEquals(ErrorCode.INVALID_RESET_TOKEN, assertThrows(BusinessException.class,
                    () -> service.reset(new ResetPasswordCommand(token, "Password123"))).getErrorCode());
        }
        verifyNoInteractions(encoder, events);
        verify(users, never()).resetRecoveredPassword(any());
    }

    @Test
    void changedEmailPasswordOrDisabledAccountRejectsToken() {
        when(mapper.findToken(anyString())).thenReturn(new PasswordRecoveryMapper.Token(1L,
                RecoveryTokens.hash(token), "user@example.org", RecoveryTokens.hash("old-hash"),
                Instant.now().plusSeconds(100)));
        when(mapper.lockToken(anyString())).thenReturn(new PasswordRecoveryMapper.Token(1L,
                RecoveryTokens.hash(token), "user@example.org", RecoveryTokens.hash("old-hash"),
                Instant.now().plusSeconds(100)));
        UserEntity user = new UserEntity();
        user.setStatus(UserStatus.ACTIVE);
        user.setEmail("changed@example.org");
        user.setPasswordHash("old-hash");
        when(users.selectRecoveryUserForUpdate(1L)).thenReturn(user);
        assertThrows(BusinessException.class, () -> service.reset(new ResetPasswordCommand(token, "Password123")));
        user.setEmail("user@example.org");
        user.setPasswordHash("changed-hash");
        assertThrows(BusinessException.class, () -> service.reset(new ResetPasswordCommand(token, "Password123")));
        user.setPasswordHash("old-hash");
        user.setStatus(UserStatus.DISABLED);
        assertThrows(BusinessException.class, () -> service.reset(new ResetPasswordCommand(token, "Password123")));
        verify(users, never()).resetRecoveredPassword(any());
    }

    @Test
    void weakOrOversizedUtf8PasswordRejectedBeforeDatabaseLookup() {
        for (String password : new String[]{"short1", "onlyletters", "密".repeat(24) + "A1"}) {
            assertThrows(BusinessException.class, () -> service.reset(new ResetPasswordCommand(token, password)));
        }
        verifyNoInteractions(mapper, users, encoder);
    }

    @Test
    void resetConsumesTokenAndRevokesEverySession() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setAccount("requester");
        user.setEmail("user@example.org");
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("old-hash");
        when(mapper.findToken(anyString())).thenReturn(new PasswordRecoveryMapper.Token(1L,
                RecoveryTokens.hash(token), user.getEmail(), RecoveryTokens.hash("old-hash"),
                Instant.now().plusSeconds(900)));
        when(mapper.lockToken(anyString())).thenReturn(new PasswordRecoveryMapper.Token(1L,
                RecoveryTokens.hash(token), user.getEmail(), RecoveryTokens.hash("old-hash"),
                Instant.now().plusSeconds(900)));
        when(users.selectRecoveryUserForUpdate(1L)).thenReturn(user);
        when(encoder.encode("Password123")).thenReturn("new-hash");
        when(users.resetRecoveredPassword(user)).thenReturn(1);
        when(mapper.deleteToken(RecoveryTokens.hash(token))).thenReturn(1);
        service.reset(new ResetPasswordCommand(token, "Password123"));
        assertEquals("new-hash", user.getPasswordHash());
        verify(events).publishEvent(new PasswordChangedEvent("requester", null));
        verify(audit).record(eq(1L), eq("PASSWORD_RESET"), eq("USER"), eq("1"), isNull(), any());
    }
}
