package cn.edu.techgroup.outsourcing.modules.user.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.user.dto.CreateMemberCommand;
import cn.edu.techgroup.outsourcing.modules.user.dto.UpdateMemberCommand;
import cn.edu.techgroup.outsourcing.modules.user.dto.UpdateMemberStatusCommand;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserStatus;
import cn.edu.techgroup.outsourcing.modules.user.event.MemberAccessChangedPublisher;
import cn.edu.techgroup.outsourcing.modules.user.mapper.SkillTagMapper;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserSkillMapper;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminMemberServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private SkillTagMapper skillTagMapper;
    @Mock
    private UserSkillMapper userSkillMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditRecorder auditRecorder;
    @Mock
    private MemberAccessChangedPublisher accessChangedPublisher;

    private AdminMemberServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminMemberServiceImpl(
                userMapper,
                skillTagMapper,
                userSkillMapper,
                passwordEncoder,
                auditRecorder,
                accessChangedPublisher);
    }

    @Test
    void createHashesPasswordAndAuditsWithoutPasswordData() {
        when(passwordEncoder.encode("Password1")).thenReturn("hash");
        when(userMapper.insert(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(4L);
            return 1;
        });

        var result = service.create(
                createCommand("Password1", "新增技术组成员账号"),
                admin(9L));

        assertEquals("4", result.id());
        assertEquals(0, result.updatedAt().getNano() % 1_000_000);
        ArgumentCaptor<UserEntity> inserted =
                ArgumentCaptor.forClass(UserEntity.class);
        verify(userMapper).insert(inserted.capture());
        assertEquals("hash", inserted.getValue().getPasswordHash());
        verify(auditRecorder).record(
                any(),
                eq("MEMBER_CREATE"),
                eq("USER"),
                eq("4"),
                isNull(),
                any());
    }

    @Test
    void updateCanClearNullableProfileFields() {
        UserEntity member = member(4L, UserRole.MEMBER, UserStatus.ACTIVE);
        member.setEmail("member@example.edu");
        member.setPhone("13800000000");
        member.setDepartment("计算机学院");
        when(userMapper.selectTargetAndActiveAdminsForUpdate(4L))
                .thenReturn(List.of(member));
        when(userSkillMapper.selectByUserIds(anyCollection())).thenReturn(List.of());
        when(userMapper.updateAdminProfile(any(UserEntity.class))).thenReturn(1);

        var result = service.update(
                4L,
                new UpdateMemberCommand(
                        member.getUpdatedAt(),
                        "成员四",
                        null,
                        " ",
                        null,
                        UserRole.MEMBER,
                        List.of(),
                        "清理已经失效的联系方式"),
                admin(9L));

        assertNull(result.email());
        assertNull(result.phone());
        assertNull(result.department());
        ArgumentCaptor<UserEntity> updated =
                ArgumentCaptor.forClass(UserEntity.class);
        verify(userMapper).updateAdminProfile(updated.capture());
        assertNull(updated.getValue().getEmail());
        assertNull(updated.getValue().getPhone());
        assertNull(updated.getValue().getDepartment());
        verify(auditRecorder).record(
                any(),
                eq("MEMBER_UPDATE"),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    void staleExpectedUpdatedAtUsesDataVersionConflict() {
        UserEntity member = member(4L, UserRole.MEMBER, UserStatus.ACTIVE);
        when(userMapper.selectTargetAndActiveAdminsForUpdate(4L))
                .thenReturn(List.of(member));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.update(
                        4L,
                        new UpdateMemberCommand(
                                member.getUpdatedAt().minusSeconds(1),
                                "成员四",
                                null,
                                null,
                                null,
                                UserRole.MEMBER,
                                List.of(),
                                "更新技术组成员资料"),
                        admin(9L)));

        assertSame(ErrorCode.DATA_VERSION_CONFLICT, exception.getErrorCode());
        verify(userMapper, never()).updateAdminProfile(any(UserEntity.class));
    }

    @Test
    void displayNameChangePublishesSessionInvalidationEvent() {
        UserEntity member = member(4L, UserRole.MEMBER, UserStatus.ACTIVE);
        member.setDisplayName("旧显示名称");
        when(userMapper.selectTargetAndActiveAdminsForUpdate(4L))
                .thenReturn(List.of(member));
        when(userSkillMapper.selectByUserIds(anyCollection())).thenReturn(List.of());
        when(userMapper.updateAdminProfile(any(UserEntity.class))).thenReturn(1);

        service.update(
                4L,
                new UpdateMemberCommand(
                        member.getUpdatedAt(),
                        "新显示名称",
                        null,
                        null,
                        null,
                        UserRole.MEMBER,
                        List.of(),
                        "修正技术组成员显示名称"),
                admin(9L));

        verify(accessChangedPublisher).publish(member.getAccount());
    }

    @Test
    void enablingMemberClearsLoginLockAndSessions() {
        UserEntity member = member(4L, UserRole.MEMBER, UserStatus.DISABLED);
        member.setFailedLoginCount(5);
        member.setLockedUntil(Instant.parse("2026-08-15T10:00:00Z"));
        when(userMapper.selectTargetAndActiveAdminsForUpdate(4L))
                .thenReturn(List.of(member));
        when(userSkillMapper.selectByUserIds(anyCollection())).thenReturn(List.of());
        when(userMapper.updateAdminStatus(any(UserEntity.class))).thenReturn(1);

        service.updateStatus(
                4L,
                new UpdateMemberStatusCommand(
                        member.getUpdatedAt(),
                        UserStatus.ACTIVE,
                        "重新启用该技术组成员账号"),
                admin(9L));

        ArgumentCaptor<UserEntity> updated =
                ArgumentCaptor.forClass(UserEntity.class);
        verify(userMapper).updateAdminStatus(updated.capture());
        assertEquals(0, updated.getValue().getFailedLoginCount());
        assertNull(updated.getValue().getLockedUntil());
        verify(accessChangedPublisher).publish(member.getAccount());
        verify(auditRecorder).record(
                any(),
                eq("MEMBER_STATUS"),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    void cannotDisableSelf() {
        UserEntity self = member(9L, UserRole.ADMIN, UserStatus.ACTIVE);
        when(userMapper.selectTargetAndActiveAdminsForUpdate(9L))
                .thenReturn(List.of(self));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.updateStatus(
                        9L,
                        new UpdateMemberStatusCommand(
                                self.getUpdatedAt(),
                                UserStatus.DISABLED,
                                "管理员不能停用自己的账号"),
                        admin(9L)));

        assertSame(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
        verify(userMapper, never()).updateAdminStatus(any(UserEntity.class));
    }

    @Test
    void lastActiveAdminIsProtected() {
        UserEntity lastAdmin = member(8L, UserRole.ADMIN, UserStatus.ACTIVE);
        when(userMapper.selectTargetAndActiveAdminsForUpdate(8L))
                .thenReturn(List.of(lastAdmin));
        when(userMapper.countActiveOwnerRequests(8L)).thenReturn(0L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.updateStatus(
                        8L,
                        new UpdateMemberStatusCommand(
                                lastAdmin.getUpdatedAt(),
                                UserStatus.DISABLED,
                                "不能移除最后一个管理员"),
                        admin(9L)));

        assertSame(ErrorCode.REQUEST_STATUS_CONFLICT, exception.getErrorCode());
        verify(accessChangedPublisher, never()).publish(any());
    }

    @Test
    void activeOwnerRequestsAreLockedAndRecheckedBeforeDisable() {
        UserEntity member = member(4L, UserRole.MEMBER, UserStatus.ACTIVE);
        when(userMapper.selectTargetAndActiveAdminsForUpdate(4L))
                .thenReturn(List.of(member));
        when(userMapper.countActiveOwnerRequests(4L)).thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.updateStatus(
                        4L,
                        new UpdateMemberStatusCommand(
                                member.getUpdatedAt(),
                                UserStatus.DISABLED,
                                "成员仍有正在负责的需求任务"),
                        admin(9L)));

        assertSame(ErrorCode.REQUEST_STATUS_CONFLICT, exception.getErrorCode());
        verify(userMapper).lockActiveOwnerRequestIds(4L);
        verify(userMapper, never()).updateAdminStatus(any(UserEntity.class));
    }

    @Test
    void reasonMustContainFiveCharactersAfterTrim() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.create(
                        createCommand("Password1", "a    "),
                        admin(9L)));

        assertSame(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
        verifyNoInteractions(passwordEncoder, userMapper);
    }

    @Test
    void bcryptPasswordCannotExceed72Utf8Bytes() {
        String oversizedUtf8Password = "A1" + "汉".repeat(24);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.create(
                        createCommand(
                                oversizedUtf8Password,
                                "新增技术组成员账号"),
                        admin(9L)));

        assertSame(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
        verifyNoInteractions(passwordEncoder);
    }

    private CreateMemberCommand createCommand(String password, String reason) {
        return new CreateMemberCommand(
                "member4",
                password,
                "成员四",
                null,
                null,
                null,
                UserRole.MEMBER,
                List.of(),
                reason);
    }

    private UserEntity member(
            Long id,
            UserRole role,
            UserStatus status) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setAccount("user" + id);
        user.setDisplayName("成员" + id);
        user.setRole(role);
        user.setStatus(status);
        user.setFailedLoginCount(0);
        user.setCreatedAt(Instant.parse("2026-08-14T09:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-08-14T10:00:00Z"));
        return user;
    }

    private LoginUser admin(Long id) {
        return new LoginUser(
                id,
                "admin",
                "hash",
                "管理员",
                UserRole.ADMIN,
                true,
                true);
    }
}
