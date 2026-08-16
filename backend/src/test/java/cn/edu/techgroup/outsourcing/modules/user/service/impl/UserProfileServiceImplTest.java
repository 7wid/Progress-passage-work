package cn.edu.techgroup.outsourcing.modules.user.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.user.dto.ChangePasswordCommand;
import cn.edu.techgroup.outsourcing.modules.user.dto.UpdateMyProfileCommand;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.event.PasswordChangedEvent;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditRecorder auditRecorder;
    @Mock private ApplicationEventPublisher eventPublisher;

    private UserProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserProfileServiceImpl(
                userMapper,
                passwordEncoder,
                auditRecorder,
                eventPublisher);
    }

    @Test
    void updatesNormalizedProfile() {
        UserEntity user = user();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.updateSelfProfile(user)).thenReturn(1);

        var result = service.update(
                new UpdateMyProfileCommand(
                        " 新名称 ", " USER@EXAMPLE.COM ", " 13800000000 ", " 计算机学院 "),
                loginUser());

        assertEquals("新名称", result.displayName());
        assertEquals("user@example.com", result.email());
        assertEquals("13800000000", result.phone());
    }

    @Test
    void rejectsWrongCurrentPassword() {
        UserEntity user = user();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.changePassword(
                        new ChangePasswordCommand("wrong", "newPassword1"),
                        "current-session",
                        loginUser()));

        assertSame(ErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
    }

    @Test
    void changesPasswordAndPublishesSessionEvent() {
        UserEntity user = user();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("oldPassword1", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("newPassword2", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("newPassword2")).thenReturn("new-hash");
        when(userMapper.updatePasswordHash(user)).thenReturn(1);

        var result = service.changePassword(
                new ChangePasswordCommand("oldPassword1", "newPassword2"),
                "current-session",
                loginUser());

        assertEquals(true, result.otherSessionsInvalidated());
        verify(eventPublisher).publishEvent(any(PasswordChangedEvent.class));
        assertEquals("new-hash", user.getPasswordHash());
    }

    private UserEntity user() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setAccount("requester");
        user.setPasswordHash("old-hash");
        user.setDisplayName("需求方");
        user.setRole(UserRole.REQUESTER);
        return user;
    }

    private LoginUser loginUser() {
        return new LoginUser(
                1L,
                "requester",
                "old-hash",
                "需求方",
                UserRole.REQUESTER,
                true,
                true);
    }
}
