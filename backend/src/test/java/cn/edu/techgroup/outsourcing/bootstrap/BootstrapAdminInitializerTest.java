package cn.edu.techgroup.outsourcing.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.edu.techgroup.outsourcing.config.BootstrapAdminProperties;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserStatus;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

class BootstrapAdminInitializerTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createsAdminWithEncodedPassword() {
        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("StrongPassword123")).thenReturn("encoded");
        when(userMapper.insert(any(UserEntity.class))).thenReturn(1);

        initializer("StrongPassword123").run(null);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userMapper).insert(captor.capture());
        UserEntity created = captor.getValue();
        assertEquals("admin", created.getAccount());
        assertEquals("encoded", created.getPasswordHash());
        assertEquals("系统管理员", created.getDisplayName());
        assertEquals(UserRole.ADMIN, created.getRole());
        assertEquals(UserStatus.ACTIVE, created.getStatus());
        assertEquals(0, created.getFailedLoginCount());
    }

    @Test
    void existingAdminDoesNotRequireBootstrapPassword() {
        UserEntity existing = new UserEntity();
        existing.setAccount("admin");
        existing.setRole(UserRole.ADMIN);
        when(userMapper.selectOne(any())).thenReturn(existing);

        initializer(null).run(null);

        verify(userMapper, never()).insert(any(UserEntity.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void rejectsWeakPasswordBeforeWritingUser() {
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> initializer("onlyletters").run(null));

        verify(userMapper, never()).insert(any(UserEntity.class));
        verifyNoInteractions(passwordEncoder);
    }

    private BootstrapAdminInitializer initializer(String password) {
        return new BootstrapAdminInitializer(
                userMapper,
                passwordEncoder,
                new BootstrapAdminProperties(
                        true,
                        "admin",
                        password,
                        "系统管理员"));
    }
}
