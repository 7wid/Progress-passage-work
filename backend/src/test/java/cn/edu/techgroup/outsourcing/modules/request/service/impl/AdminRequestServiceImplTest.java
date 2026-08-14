package cn.edu.techgroup.outsourcing.modules.request.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.assignment.entity.RequestMemberEntity;
import cn.edu.techgroup.outsourcing.modules.assignment.enums.RequestMemberType;
import cn.edu.techgroup.outsourcing.modules.assignment.mapper.RequestMemberMapper;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.notification.event.NotificationEventPublisher;
import cn.edu.techgroup.outsourcing.modules.progress.entity.StatusHistoryEntity;
import cn.edu.techgroup.outsourcing.modules.progress.mapper.StatusHistoryMapper;
import cn.edu.techgroup.outsourcing.modules.request.dto.AdminRequestActionCommand;
import cn.edu.techgroup.outsourcing.modules.request.entity.RequestEntity;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import cn.edu.techgroup.outsourcing.modules.request.mapper.RequestMapper;
import cn.edu.techgroup.outsourcing.modules.request.vo.AdminRequestActionVO;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserStatus;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminRequestServiceImplTest {

    @Mock
    private RequestMapper requestMapper;
    @Mock
    private RequestMemberMapper requestMemberMapper;
    @Mock
    private StatusHistoryMapper statusHistoryMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AuditRecorder auditRecorder;
    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    private AdminRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminRequestServiceImpl(
                requestMapper,
                requestMemberMapper,
                statusHistoryMapper,
                userMapper,
                auditRecorder,
                notificationEventPublisher);
    }

    @Test
    void cancelUsesCasHistoryAuditAndNotification() {
        RequestEntity request = request(RequestStatus.IN_PROGRESS, 3);
        when(requestMapper.selectByIdForUpdate(1L)).thenReturn(request);
        when(requestMapper.compareAndSetStatus(
                1L,
                "IN_PROGRESS",
                "CANCELLED",
                3)).thenReturn(1);
        when(statusHistoryMapper.insert(any(StatusHistoryEntity.class)))
                .thenReturn(1);
        when(requestMemberMapper.selectByRequestId(1L)).thenReturn(List.of());

        AdminRequestActionVO result = service.cancel(
                1L,
                new AdminRequestActionCommand(
                        3,
                        "长期无反馈，线下确认取消"),
                admin());

        assertSame(RequestStatus.CANCELLED, result.status());
        assertEquals(4, result.version());
        verify(auditRecorder).record(
                any(),
                eq("REQUEST_CANCEL"),
                any(),
                any(),
                any(),
                any());
        verify(notificationEventPublisher).publish(any());
    }

    @Test
    void terminalRequestCannotBeCancelled() {
        when(requestMapper.selectByIdForUpdate(1L))
                .thenReturn(request(RequestStatus.COMPLETED, 3));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.cancel(
                        1L,
                        command(3),
                        admin()));

        assertSame(ErrorCode.REQUEST_STATUS_CONFLICT, exception.getErrorCode());
        verify(statusHistoryMapper, never()).insert(any(StatusHistoryEntity.class));
    }

    @Test
    void rejectedRequestReopensForReview() {
        stubSuccessfulTransition(
                RequestStatus.REJECTED,
                RequestStatus.PENDING_REVIEW,
                2,
                List.of());

        AdminRequestActionVO result = service.reopen(
                1L,
                command(2),
                admin());

        assertSame(RequestStatus.PENDING_REVIEW, result.status());
    }

    @Test
    void completedRequestWithActiveOwnerReopensInProgress() {
        RequestMemberEntity owner = owner(5L);
        stubSuccessfulTransition(
                RequestStatus.COMPLETED,
                RequestStatus.IN_PROGRESS,
                2,
                List.of(owner));
        when(userMapper.selectAssignmentUsersByIdsForUpdate(List.of(5L)))
                .thenReturn(List.of(user(5L, UserStatus.ACTIVE)));

        AdminRequestActionVO result = service.reopen(
                1L,
                command(2),
                admin());

        assertSame(RequestStatus.IN_PROGRESS, result.status());
    }

    @Test
    void completedRequestWithDisabledOwnerRequiresAssignment() {
        RequestMemberEntity owner = owner(5L);
        stubSuccessfulTransition(
                RequestStatus.COMPLETED,
                RequestStatus.PENDING_ASSIGNMENT,
                2,
                List.of(owner));
        when(userMapper.selectAssignmentUsersByIdsForUpdate(List.of(5L)))
                .thenReturn(List.of(user(5L, UserStatus.DISABLED)));

        AdminRequestActionVO result = service.reopen(
                1L,
                command(2),
                admin());

        assertSame(RequestStatus.PENDING_ASSIGNMENT, result.status());
    }

    @Test
    void cancelledRequestRestoresItsRecordedStatus() {
        StatusHistoryEntity latestCancellation = new StatusHistoryEntity();
        latestCancellation.setFromStatus(RequestStatus.NEED_MORE_INFO);
        when(statusHistoryMapper.selectLatestCancellation(1L))
                .thenReturn(latestCancellation);
        stubSuccessfulTransition(
                RequestStatus.CANCELLED,
                RequestStatus.NEED_MORE_INFO,
                2,
                List.of());

        AdminRequestActionVO result = service.reopen(
                1L,
                command(2),
                admin());

        assertSame(RequestStatus.NEED_MORE_INFO, result.status());
    }

    @Test
    void cancelledInProgressRequestWithInactiveOwnerRequiresAssignment() {
        StatusHistoryEntity latestCancellation = new StatusHistoryEntity();
        latestCancellation.setFromStatus(RequestStatus.IN_PROGRESS);
        when(statusHistoryMapper.selectLatestCancellation(1L))
                .thenReturn(latestCancellation);
        RequestMemberEntity owner = owner(5L);
        stubSuccessfulTransition(
                RequestStatus.CANCELLED,
                RequestStatus.PENDING_ASSIGNMENT,
                2,
                List.of(owner));
        when(userMapper.selectAssignmentUsersByIdsForUpdate(List.of(5L)))
                .thenReturn(List.of(user(5L, UserStatus.DISABLED)));

        AdminRequestActionVO result = service.reopen(
                1L,
                command(2),
                admin());

        assertSame(RequestStatus.PENDING_ASSIGNMENT, result.status());
    }

    @Test
    void memberIsRejectedBeforeDatabaseAccess() {
        LoginUser member = new LoginUser(
                2L,
                "member",
                "x",
                "成员",
                UserRole.MEMBER,
                true,
                true);

        assertThrows(
                BusinessException.class,
                () -> service.cancel(1L, command(0), member));

        verifyNoInteractions(requestMapper);
    }

    private void stubSuccessfulTransition(
            RequestStatus from,
            RequestStatus to,
            int version,
            List<RequestMemberEntity> members) {
        when(requestMapper.selectByIdForUpdate(1L))
                .thenReturn(request(from, version));
        when(requestMemberMapper.selectByRequestId(1L)).thenReturn(members);
        when(requestMapper.compareAndSetStatus(
                1L,
                from.getValue(),
                to.getValue(),
                version)).thenReturn(1);
        when(statusHistoryMapper.insert(any(StatusHistoryEntity.class)))
                .thenReturn(1);
    }

    private AdminRequestActionCommand command(int version) {
        return new AdminRequestActionCommand(
                version,
                "管理员确认需要调整需求状态");
    }

    private RequestEntity request(RequestStatus status, int version) {
        RequestEntity request = new RequestEntity();
        request.setId(1L);
        request.setRequestNo("REQ-1");
        request.setCreatorId(7L);
        request.setStatus(status);
        request.setVersion(version);
        return request;
    }

    private RequestMemberEntity owner(Long userId) {
        RequestMemberEntity member = new RequestMemberEntity();
        member.setId(10L);
        member.setRequestId(1L);
        member.setUserId(userId);
        member.setMemberType(RequestMemberType.OWNER);
        return member;
    }

    private UserEntity user(Long id, UserStatus status) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setRole(UserRole.MEMBER);
        user.setStatus(status);
        return user;
    }

    private LoginUser admin() {
        return new LoginUser(
                9L,
                "admin",
                "x",
                "管理员",
                UserRole.ADMIN,
                true,
                true);
    }
}
