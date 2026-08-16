package cn.edu.techgroup.outsourcing.modules.user.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.config.RegistrationProperties;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.user.dto.RegisterUserCommand;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditRecorder auditRecorder;

    @Test
    void registersRequesterWithNormalizedAccountAndEmail() {
        var service = service(true, "example.edu.cn");
        when(passwordEncoder.encode("Password1")).thenReturn("hash");
        when(userMapper.insert(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        });

        var result = service.register(new RegisterUserCommand(
                " Student.01 ",
                "Password1",
                " 学生用户 ",
                " STUDENT@EXAMPLE.EDU.CN ",
                null,
                " 计算机学院 "));

        assertEquals("student.01", result.account());
        assertEquals("student@example.edu.cn", result.email());
        assertEquals(UserRole.REQUESTER, result.role());
        verify(auditRecorder).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsRegistrationWhenDisabled() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service(false, null).register(command("user@example.edu.cn")));

        assertSame(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void rejectsEmailOutsideConfiguredSuffix() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service(true, "@example.edu.cn").register(command("user@other.edu.cn")));

        assertSame(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
    }

    private UserRegistrationServiceImpl service(boolean enabled, String suffix) {
        return new UserRegistrationServiceImpl(
                new RegistrationProperties(enabled, suffix),
                userMapper,
                passwordEncoder,
                auditRecorder);
    }

    private RegisterUserCommand command(String email) {
        return new RegisterUserCommand(
                "student01",
                "Password1",
                "学生用户",
                email,
                null,
                null);
    }
}
