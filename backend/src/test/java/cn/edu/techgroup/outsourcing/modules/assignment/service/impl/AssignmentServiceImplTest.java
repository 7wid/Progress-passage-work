package cn.edu.techgroup.outsourcing.modules.assignment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.assignment.dto.UpdateRequestMembersCommand;
import cn.edu.techgroup.outsourcing.modules.assignment.entity.RequestMemberEntity;
import cn.edu.techgroup.outsourcing.modules.assignment.enums.RequestMemberType;
import cn.edu.techgroup.outsourcing.modules.assignment.mapper.RequestMemberMapper;
import cn.edu.techgroup.outsourcing.modules.assignment.vo.RequestAssignmentVO;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.notification.event.NotificationEventPublisher;
import cn.edu.techgroup.outsourcing.modules.notification.enums.NotificationType;
import cn.edu.techgroup.outsourcing.modules.progress.entity.StatusHistoryEntity;
import cn.edu.techgroup.outsourcing.modules.progress.mapper.StatusHistoryMapper;
import cn.edu.techgroup.outsourcing.modules.request.entity.RequestEntity;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import cn.edu.techgroup.outsourcing.modules.request.mapper.RequestMapper;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserStatus;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceImplTest {

    private static final Long REQUEST_ID = 100L;

    @Mock
    private RequestMapper requestMapper;
    @Mock
    private RequestMemberMapper requestMemberMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private StatusHistoryMapper statusHistoryMapper;
    @Mock
    private NotificationEventPublisher notificationEventPublisher;
    @Mock
    private AuditRecorder auditRecorder;

    private AssignmentServiceImpl assignmentService;

    @BeforeEach
    void setUp() {
        assignmentService = new AssignmentServiceImpl(
                requestMapper,
                requestMemberMapper,
                userMapper,
                statusHistoryMapper,
                notificationEventPublisher,
                auditRecorder);
    }

    @Test
    void initialAssignmentMovesRequestToInProgress() {
        RequestMemberEntity owner = relation(
                11L,
                2L,
                RequestMemberType.OWNER);
        RequestMemberEntity participant = relation(
                12L,
                3L,
                RequestMemberType.PARTICIPANT);

        when(requestMapper.selectByIdForUpdate(REQUEST_ID))
                .thenReturn(request(RequestStatus.PENDING_ASSIGNMENT, 0));
        when(userMapper.selectAssignmentUsersByIdsForUpdate(anyCollection()))
                .thenReturn(List.of(
                        user(2L, UserRole.MEMBER, UserStatus.ACTIVE),
                        user(3L, UserRole.MEMBER, UserStatus.ACTIVE)));
        when(requestMemberMapper.selectByRequestId(REQUEST_ID))
                .thenReturn(List.of())
                .thenReturn(List.of(owner, participant));
        when(requestMapper.compareAndSetStatus(
                REQUEST_ID,
                "PENDING_ASSIGNMENT",
                "IN_PROGRESS",
                0))
                .thenReturn(1);
        when(requestMemberMapper.insert(any(RequestMemberEntity.class)))
                .thenReturn(1);
        when(statusHistoryMapper.insert(any(StatusHistoryEntity.class)))
                .thenReturn(1);

        RequestAssignmentVO result = assignmentService.update(
                REQUEST_ID,
                command(0, 2L, List.of(3L)),
                loginUser(9L, UserRole.ADMIN));

        assertSame(RequestStatus.IN_PROGRESS, result.requestStatus());
        assertEquals(1, result.requestVersion());
        assertEquals("2", result.owner().userId());
        assertEquals(1, result.participants().size());
        verify(requestMemberMapper, times(2))
                .insert(any(RequestMemberEntity.class));
        verify(statusHistoryMapper).insert(any(StatusHistoryEntity.class));
        verify(notificationEventPublisher).publish(argThat(event ->
                event.type() == NotificationType.ASSIGNMENT_UPDATED
                        && event.recipientIds().equals(List.of(2L, 3L, 1L))));
    }

    @Test
    void identicalInProgressAssignmentIsIdempotent() {
        RequestMemberEntity owner = relation(
                11L,
                2L,
                RequestMemberType.OWNER);
        when(requestMapper.selectByIdForUpdate(REQUEST_ID))
                .thenReturn(request(RequestStatus.IN_PROGRESS, 3));
        when(userMapper.selectAssignmentUsersByIdsForUpdate(anyCollection()))
                .thenReturn(List.of(
                        user(2L, UserRole.MEMBER, UserStatus.ACTIVE)));
        when(requestMemberMapper.selectByRequestId(REQUEST_ID))
                .thenReturn(List.of(owner));

        RequestAssignmentVO result = assignmentService.update(
                REQUEST_ID,
                command(3, 2L, List.of()),
                loginUser(9L, UserRole.ADMIN));

        assertEquals(3, result.requestVersion());
        verify(requestMapper, never()).compareAndSetStatus(
                anyLong(), anyString(), anyString(), anyInt());
        verifyNoInteractions(statusHistoryMapper);
        verifyNoInteractions(notificationEventPublisher);
    }

    @Test
    void transferOwnerKeepsStatusAndIncrementsVersion() {
        RequestMemberEntity oldOwner = relation(
                11L,
                2L,
                RequestMemberType.OWNER);
        RequestMemberEntity newOwner = relation(
                12L,
                3L,
                RequestMemberType.OWNER);
        UserEntity oldOwnerUser = user(
                2L,
                UserRole.MEMBER,
                UserStatus.ACTIVE);

        when(requestMapper.selectByIdForUpdate(REQUEST_ID))
                .thenReturn(request(RequestStatus.IN_PROGRESS, 1));
        when(userMapper.selectAssignmentUsersByIdsForUpdate(anyCollection()))
                .thenReturn(List.of(
                        user(3L, UserRole.MEMBER, UserStatus.ACTIVE)));
        when(requestMemberMapper.selectByRequestId(REQUEST_ID))
                .thenReturn(List.of(oldOwner))
                .thenReturn(List.of(newOwner));
        when(userMapper.selectById(2L)).thenReturn(oldOwnerUser);
        when(requestMapper.compareAndSetStatus(
                REQUEST_ID,
                "IN_PROGRESS",
                "IN_PROGRESS",
                1))
                .thenReturn(1);
        when(requestMemberMapper.deleteById(11L)).thenReturn(1);
        when(requestMemberMapper.insert(any(RequestMemberEntity.class)))
                .thenReturn(1);
        when(statusHistoryMapper.insert(any(StatusHistoryEntity.class)))
                .thenReturn(1);

        RequestAssignmentVO result = assignmentService.update(
                REQUEST_ID,
                command(1, 3L, List.of()),
                loginUser(9L, UserRole.ADMIN));

        assertSame(RequestStatus.IN_PROGRESS, result.requestStatus());
        assertEquals(2, result.requestVersion());
        assertEquals("3", result.owner().userId());
    }

    @Test
    void rejectsNonAdminBeforeAccessingDatabase() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> assignmentService.update(
                        REQUEST_ID,
                        command(0, 2L, List.of()),
                        loginUser(2L, UserRole.MEMBER)));

        assertSame(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
        verifyNoInteractions(
                requestMapper,
                requestMemberMapper,
                userMapper,
                statusHistoryMapper);
    }

    @Test
    void rejectsDuplicateParticipants() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> assignmentService.update(
                        REQUEST_ID,
                        command(0, 2L, List.of(3L, 3L)),
                        loginUser(9L, UserRole.ADMIN)));

        assertSame(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
        verifyNoInteractions(
                requestMapper,
                requestMemberMapper,
                userMapper,
                statusHistoryMapper);
    }

    @Test
    void rejectsStaleRequestVersion() {
        when(requestMapper.selectByIdForUpdate(REQUEST_ID))
                .thenReturn(request(RequestStatus.PENDING_ASSIGNMENT, 2));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> assignmentService.update(
                        REQUEST_ID,
                        command(1, 2L, List.of()),
                        loginUser(9L, UserRole.ADMIN)));

        assertSame(ErrorCode.REQUEST_STATUS_CONFLICT, exception.getErrorCode());
        verifyNoInteractions(
                requestMemberMapper,
                userMapper,
                statusHistoryMapper);
    }

    @Test
    void rejectsDisabledTargetUser() {
        when(requestMapper.selectByIdForUpdate(REQUEST_ID))
                .thenReturn(request(RequestStatus.PENDING_ASSIGNMENT, 0));
        when(userMapper.selectAssignmentUsersByIdsForUpdate(anyCollection()))
                .thenReturn(List.of(
                        user(2L, UserRole.MEMBER, UserStatus.DISABLED)));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> assignmentService.update(
                        REQUEST_ID,
                        command(0, 2L, List.of()),
                        loginUser(9L, UserRole.ADMIN)));

        assertSame(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
        verifyNoInteractions(requestMemberMapper, statusHistoryMapper);
    }

    @Test
    void reportsConflictWhenCompareAndSetFails() {
        when(requestMapper.selectByIdForUpdate(REQUEST_ID))
                .thenReturn(request(RequestStatus.PENDING_ASSIGNMENT, 0));
        when(userMapper.selectAssignmentUsersByIdsForUpdate(anyCollection()))
                .thenReturn(List.of(
                        user(2L, UserRole.MEMBER, UserStatus.ACTIVE)));
        when(requestMemberMapper.selectByRequestId(REQUEST_ID))
                .thenReturn(List.of());
        when(requestMapper.compareAndSetStatus(
                REQUEST_ID,
                "PENDING_ASSIGNMENT",
                "IN_PROGRESS",
                0))
                .thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> assignmentService.update(
                        REQUEST_ID,
                        command(0, 2L, List.of()),
                        loginUser(9L, UserRole.ADMIN)));

        assertSame(ErrorCode.REQUEST_STATUS_CONFLICT, exception.getErrorCode());
        verify(requestMemberMapper, never())
                .insert(any(RequestMemberEntity.class));
        verifyNoInteractions(statusHistoryMapper);
    }

    private UpdateRequestMembersCommand command(
            int version,
            Long ownerId,
            List<Long> participantIds) {
        return new UpdateRequestMembersCommand(
                version,
                ownerId,
                participantIds,
                "根据技术方向调整负责人");
    }

    private RequestEntity request(RequestStatus status, int version) {
        RequestEntity request = new RequestEntity();
        request.setId(REQUEST_ID);
        request.setCreatorId(1L);
        request.setStatus(status);
        request.setVersion(version);
        return request;
    }

    private UserEntity user(
            Long id,
            UserRole role,
            UserStatus status) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setAccount("user-" + id);
        user.setDisplayName("用户" + id);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }

    private RequestMemberEntity relation(
            Long id,
            Long userId,
            RequestMemberType memberType) {
        RequestMemberEntity member = new RequestMemberEntity();
        member.setId(id);
        member.setRequestId(REQUEST_ID);
        member.setUserId(userId);
        member.setMemberType(memberType);
        member.setJoinedAt(Instant.parse("2026-08-11T08:00:00Z"));
        return member;
    }

    private LoginUser loginUser(Long id, UserRole role) {
        return new LoginUser(
                id,
                "test-user",
                "password-hash",
                "测试用户",
                role,
                true,
                true);
    }
}
