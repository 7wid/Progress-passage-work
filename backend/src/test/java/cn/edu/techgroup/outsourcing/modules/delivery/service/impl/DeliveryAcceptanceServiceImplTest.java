package cn.edu.techgroup.outsourcing.modules.delivery.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.assignment.entity.RequestMemberEntity;
import cn.edu.techgroup.outsourcing.modules.assignment.enums.RequestMemberType;
import cn.edu.techgroup.outsourcing.modules.assignment.mapper.RequestMemberMapper;
import cn.edu.techgroup.outsourcing.modules.delivery.dto.CreateAcceptanceCommand;
import cn.edu.techgroup.outsourcing.modules.delivery.dto.CreateDeliveryCommand;
import cn.edu.techgroup.outsourcing.modules.delivery.entity.AcceptanceEntity;
import cn.edu.techgroup.outsourcing.modules.delivery.entity.DeliveryEntity;
import cn.edu.techgroup.outsourcing.modules.delivery.enums.AcceptanceResult;
import cn.edu.techgroup.outsourcing.modules.delivery.mapper.AcceptanceMapper;
import cn.edu.techgroup.outsourcing.modules.delivery.mapper.DeliveryMapper;
import cn.edu.techgroup.outsourcing.modules.delivery.vo.CreatedAcceptanceResultVO;
import cn.edu.techgroup.outsourcing.modules.delivery.vo.CreatedDeliveryResultVO;
import cn.edu.techgroup.outsourcing.modules.delivery.vo.DeliveryAcceptanceSnapshotVO;
import cn.edu.techgroup.outsourcing.modules.progress.entity.StatusHistoryEntity;
import cn.edu.techgroup.outsourcing.modules.file.service.AttachmentService;
import cn.edu.techgroup.outsourcing.modules.notification.event.NotificationEventPublisher;
import cn.edu.techgroup.outsourcing.modules.notification.enums.NotificationType;
import cn.edu.techgroup.outsourcing.modules.progress.mapper.StatusHistoryMapper;
import cn.edu.techgroup.outsourcing.modules.request.entity.RequestEntity;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import cn.edu.techgroup.outsourcing.modules.request.mapper.RequestMapper;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
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
class DeliveryAcceptanceServiceImplTest {

    private static final Long REQUEST_ID = 100L;

    @Mock
    private RequestMapper requestMapper;
    @Mock
    private RequestMemberMapper requestMemberMapper;
    @Mock
    private DeliveryMapper deliveryMapper;
    @Mock
    private AcceptanceMapper acceptanceMapper;
    @Mock
    private StatusHistoryMapper statusHistoryMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AttachmentService attachmentService;
    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    private DeliveryAcceptanceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DeliveryAcceptanceServiceImpl(
                requestMapper,
                requestMemberMapper,
                deliveryMapper,
                acceptanceMapper,
                statusHistoryMapper,
                userMapper,
                attachmentService,
                notificationEventPublisher);
    }

    @Test
    void requesterReadsOwnHistoryAndCanAcceptLatestDelivery() {
        DeliveryEntity delivery = delivery(21L, 2L);
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.PENDING_ACCEPTANCE, 4));
        when(deliveryMapper.selectByRequestId(REQUEST_ID))
                .thenReturn(List.of(delivery));
        when(acceptanceMapper.selectByRequestId(REQUEST_ID))
                .thenReturn(List.of());
        when(userMapper.selectAssignmentUsersByIds(anyCollection()))
                .thenReturn(List.of(user(2L, "负责人")));

        DeliveryAcceptanceSnapshotVO result = service.get(
                REQUEST_ID,
                loginUser(1L, UserRole.REQUESTER));

        assertTrue(result.canAccept());
        assertFalse(result.canSubmitDelivery());
        assertEquals("负责人", result.deliveries().getFirst().submitterName());
        verifyNoInteractions(requestMemberMapper);
    }

    @Test
    void ownerCanSubmitOnlyWhileInProgress() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.IN_PROGRESS, 4));
        when(requestMemberMapper.selectByRequestId(REQUEST_ID))
                .thenReturn(List.of(member(2L, RequestMemberType.OWNER)));
        when(deliveryMapper.selectByRequestId(REQUEST_ID))
                .thenReturn(List.of());
        when(acceptanceMapper.selectByRequestId(REQUEST_ID))
                .thenReturn(List.of());

        DeliveryAcceptanceSnapshotVO result = service.get(
                REQUEST_ID,
                loginUser(2L, UserRole.MEMBER));

        assertTrue(result.canSubmitDelivery());
        assertFalse(result.canAccept());
    }

    @Test
    void participantCannotSubmitDelivery() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.IN_PROGRESS, 4));
        when(requestMemberMapper.selectByRequestId(REQUEST_ID))
                .thenReturn(List.of(member(3L, RequestMemberType.PARTICIPANT)));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createDelivery(
                        REQUEST_ID,
                        deliveryCommand(4),
                        loginUser(3L, UserRole.MEMBER)));

        assertSame(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
        verify(requestMapper, never()).compareAndSetStatus(
                any(), any(), any(), any());
        verifyNoInteractions(deliveryMapper, statusHistoryMapper);
    }

    @Test
    void ownerCreatesDeliveryAndTransitionsToPendingAcceptance() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.IN_PROGRESS, 4));
        when(requestMemberMapper.selectByRequestId(REQUEST_ID))
                .thenReturn(List.of(member(2L, RequestMemberType.OWNER)));
        when(requestMapper.compareAndSetStatus(
                REQUEST_ID,
                "IN_PROGRESS",
                "PENDING_ACCEPTANCE",
                4))
                .thenReturn(1);
        assignIdOnInsert(deliveryMapper, 21L);
        when(statusHistoryMapper.insert(any(StatusHistoryEntity.class)))
                .thenReturn(1);

        CreatedDeliveryResultVO result = service.createDelivery(
                REQUEST_ID,
                deliveryCommand(4),
                loginUser(2L, UserRole.MEMBER));

        assertSame(RequestStatus.PENDING_ACCEPTANCE, result.requestStatus());
        assertEquals(5, result.requestVersion());
        assertEquals("21", result.delivery().id());
        verify(statusHistoryMapper).insert(any(StatusHistoryEntity.class));
        verify(notificationEventPublisher).publish(argThat(event ->
                event.type() == NotificationType.DELIVERY_SUBMITTED
                        && event.recipientIds().equals(List.of(1L))));
    }

    @Test
    void adminCanSubmitDeliveryWithoutAssignmentLookup() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.IN_PROGRESS, 1));
        when(requestMapper.compareAndSetStatus(
                REQUEST_ID,
                "IN_PROGRESS",
                "PENDING_ACCEPTANCE",
                1))
                .thenReturn(1);
        assignIdOnInsert(deliveryMapper, 22L);
        when(statusHistoryMapper.insert(any(StatusHistoryEntity.class)))
                .thenReturn(1);

        service.createDelivery(
                REQUEST_ID,
                deliveryCommand(1),
                loginUser(9L, UserRole.ADMIN));

        verifyNoInteractions(requestMemberMapper);
    }

    @Test
    void invalidDeliveryUrlIsRejectedBeforeDatabaseAccess() {
        CreateDeliveryCommand command = new CreateDeliveryCommand(
                0,
                "交付说明完整",
                "file:///C:/secret.zip");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createDelivery(
                        REQUEST_ID,
                        command,
                        loginUser(9L, UserRole.ADMIN)));

        assertSame(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
        verifyNoInteractions(
                requestMapper,
                requestMemberMapper,
                deliveryMapper,
                acceptanceMapper,
                statusHistoryMapper,
                userMapper);
    }

    @Test
    void staleDeliveryVersionDoesNotWriteBusinessRows() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.IN_PROGRESS, 5));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createDelivery(
                        REQUEST_ID,
                        deliveryCommand(4),
                        loginUser(9L, UserRole.ADMIN)));

        assertSame(ErrorCode.REQUEST_STATUS_CONFLICT, exception.getErrorCode());
        verifyNoInteractions(deliveryMapper, statusHistoryMapper);
    }

    @Test
    void deliveryRejectsWrongStatusBeforeMemberLookup() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(
                        1L,
                        RequestStatus.PENDING_ACCEPTANCE,
                        4));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createDelivery(
                        REQUEST_ID,
                        deliveryCommand(4),
                        loginUser(2L, UserRole.MEMBER)));

        assertSame(ErrorCode.REQUEST_STATUS_CONFLICT, exception.getErrorCode());
        verifyNoInteractions(
                requestMemberMapper,
                deliveryMapper,
                statusHistoryMapper);
    }

    @Test
    void deliveryCasFailureDoesNotInsertDeliveryOrHistory() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.IN_PROGRESS, 4));
        when(requestMapper.compareAndSetStatus(
                REQUEST_ID,
                "IN_PROGRESS",
                "PENDING_ACCEPTANCE",
                4))
                .thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createDelivery(
                        REQUEST_ID,
                        deliveryCommand(4),
                        loginUser(9L, UserRole.ADMIN)));

        assertSame(ErrorCode.REQUEST_STATUS_CONFLICT, exception.getErrorCode());
        verifyNoInteractions(deliveryMapper, statusHistoryMapper);
    }

    @Test
    void requesterAcceptsLatestDeliveryAndCompletesRequest() {
        DeliveryEntity latest = delivery(31L, 2L);
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.PENDING_ACCEPTANCE, 5));
        when(deliveryMapper.selectLatestByRequestId(REQUEST_ID))
                .thenReturn(latest);
        when(acceptanceMapper.countByDeliveryId(31L)).thenReturn(0L);
        when(requestMapper.compareAndSetStatus(
                REQUEST_ID,
                "PENDING_ACCEPTANCE",
                "COMPLETED",
                5))
                .thenReturn(1);
        assignAcceptanceIdOnInsert(41L);
        when(statusHistoryMapper.insert(any(StatusHistoryEntity.class)))
                .thenReturn(1);
        when(requestMemberMapper.selectByRequestId(REQUEST_ID))
                .thenReturn(List.of(
                        member(2L, RequestMemberType.OWNER),
                        member(3L, RequestMemberType.PARTICIPANT)));

        CreatedAcceptanceResultVO result = service.createAcceptance(
                REQUEST_ID,
                new CreateAcceptanceCommand(
                        5,
                        AcceptanceResult.ACCEPTED,
                        null),
                loginUser(1L, UserRole.REQUESTER));

        assertSame(RequestStatus.COMPLETED, result.requestStatus());
        assertEquals("31", result.acceptance().deliveryId());
        assertEquals(6, result.requestVersion());
        verify(notificationEventPublisher).publish(argThat(event ->
                event.type() == NotificationType.ACCEPTANCE_COMPLETED
                        && event.recipientIds().equals(List.of(2L, 3L))));
    }

    @Test
    void acceptedResultAllowsOptionalShortComment() {
        DeliveryEntity latest = delivery(31L, 2L);
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.PENDING_ACCEPTANCE, 5));
        when(deliveryMapper.selectLatestByRequestId(REQUEST_ID))
                .thenReturn(latest);
        when(acceptanceMapper.countByDeliveryId(31L)).thenReturn(0L);
        when(requestMapper.compareAndSetStatus(
                REQUEST_ID,
                "PENDING_ACCEPTANCE",
                "COMPLETED",
                5))
                .thenReturn(1);
        assignAcceptanceIdOnInsert(44L);
        when(statusHistoryMapper.insert(any(StatusHistoryEntity.class)))
                .thenReturn(1);

        CreatedAcceptanceResultVO result = service.createAcceptance(
                REQUEST_ID,
                new CreateAcceptanceCommand(
                        5,
                        AcceptanceResult.ACCEPTED,
                        "好"),
                loginUser(1L, UserRole.REQUESTER));

        assertEquals("好", result.acceptance().comment());
        assertSame(RequestStatus.COMPLETED, result.requestStatus());
    }

    @Test
    void reworkReturnsRequestToInProgress() {
        DeliveryEntity latest = delivery(31L, 2L);
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.PENDING_ACCEPTANCE, 5));
        when(deliveryMapper.selectLatestByRequestId(REQUEST_ID))
                .thenReturn(latest);
        when(acceptanceMapper.countByDeliveryId(31L)).thenReturn(0L);
        when(requestMapper.compareAndSetStatus(
                REQUEST_ID,
                "PENDING_ACCEPTANCE",
                "IN_PROGRESS",
                5))
                .thenReturn(1);
        assignAcceptanceIdOnInsert(42L);
        when(statusHistoryMapper.insert(any(StatusHistoryEntity.class)))
                .thenReturn(1);

        CreatedAcceptanceResultVO result = service.createAcceptance(
                REQUEST_ID,
                new CreateAcceptanceCommand(
                        5,
                        AcceptanceResult.REWORK_REQUIRED,
                        "请修复导出功能"),
                loginUser(1L, UserRole.REQUESTER));

        assertSame(RequestStatus.IN_PROGRESS, result.requestStatus());
        assertSame(
                AcceptanceResult.REWORK_REQUIRED,
                result.acceptance().result());
    }

    @Test
    void adminAcceptsWithoutCommentAndCompletesRequest() {
        DeliveryEntity latest = delivery(31L, 2L);
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.PENDING_ACCEPTANCE, 5));
        when(deliveryMapper.selectLatestByRequestId(REQUEST_ID))
                .thenReturn(latest);
        when(acceptanceMapper.countByDeliveryId(31L)).thenReturn(0L);
        when(requestMapper.compareAndSetStatus(
                REQUEST_ID,
                "PENDING_ACCEPTANCE",
                "COMPLETED",
                5))
                .thenReturn(1);
        assignAcceptanceIdOnInsert(43L);
        when(statusHistoryMapper.insert(any(StatusHistoryEntity.class)))
                .thenReturn(1);

        CreatedAcceptanceResultVO result = service.createAcceptance(
                REQUEST_ID,
                acceptedCommand(5, null),
                loginUser(9L, UserRole.ADMIN));

        assertSame(RequestStatus.COMPLETED, result.requestStatus());
        assertEquals("31", result.acceptance().deliveryId());
    }

    @Test
    void memberCannotSubmitAcceptance() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createAcceptance(
                        REQUEST_ID,
                        acceptedCommand(5, null),
                        loginUser(2L, UserRole.MEMBER)));

        assertSame(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
        verifyNoInteractions(
                requestMapper,
                deliveryMapper,
                acceptanceMapper,
                statusHistoryMapper);
    }

    @Test
    void nonCreatorRequesterCannotAcceptAnotherRequest() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(9L, RequestStatus.PENDING_ACCEPTANCE, 5));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createAcceptance(
                        REQUEST_ID,
                        acceptedCommand(5, null),
                        loginUser(1L, UserRole.REQUESTER)));

        assertSame(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(
                deliveryMapper,
                acceptanceMapper,
                statusHistoryMapper);
    }

    @Test
    void reworkRequiresCommentBeforeDatabaseAccess() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createAcceptance(
                        REQUEST_ID,
                        new CreateAcceptanceCommand(
                                5,
                                AcceptanceResult.REWORK_REQUIRED,
                                " "),
                        loginUser(1L, UserRole.REQUESTER)));

        assertSame(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
        verifyNoInteractions(
                requestMapper,
                deliveryMapper,
                acceptanceMapper,
                statusHistoryMapper);
    }

    @Test
    void acceptanceUsesLatestDeliveryAndRejectsAlreadyAcceptedOne() {
        DeliveryEntity latest = delivery(33L, 2L);
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.PENDING_ACCEPTANCE, 5));
        when(deliveryMapper.selectLatestByRequestId(REQUEST_ID))
                .thenReturn(latest);
        when(acceptanceMapper.countByDeliveryId(33L)).thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createAcceptance(
                        REQUEST_ID,
                        acceptedCommand(5, null),
                        loginUser(1L, UserRole.REQUESTER)));

        assertSame(ErrorCode.REQUEST_STATUS_CONFLICT, exception.getErrorCode());
        verify(requestMapper, never()).compareAndSetStatus(
                any(), any(), any(), any());
        verify(acceptanceMapper, never()).insert(
                any(AcceptanceEntity.class));
        verifyNoInteractions(statusHistoryMapper);
    }

    @Test
    void missingLatestDeliveryDoesNotWriteAcceptance() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.PENDING_ACCEPTANCE, 5));
        when(deliveryMapper.selectLatestByRequestId(REQUEST_ID))
                .thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createAcceptance(
                        REQUEST_ID,
                        acceptedCommand(5, null),
                        loginUser(1L, UserRole.REQUESTER)));

        assertSame(ErrorCode.REQUEST_STATUS_CONFLICT, exception.getErrorCode());
        verifyNoInteractions(acceptanceMapper, statusHistoryMapper);
    }

    @Test
    void acceptanceCasFailureDoesNotInsertAcceptanceOrHistory() {
        DeliveryEntity latest = delivery(33L, 2L);
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.PENDING_ACCEPTANCE, 5));
        when(deliveryMapper.selectLatestByRequestId(REQUEST_ID))
                .thenReturn(latest);
        when(acceptanceMapper.countByDeliveryId(33L)).thenReturn(0L);
        when(requestMapper.compareAndSetStatus(
                REQUEST_ID,
                "PENDING_ACCEPTANCE",
                "COMPLETED",
                5))
                .thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createAcceptance(
                        REQUEST_ID,
                        acceptedCommand(5, null),
                        loginUser(1L, UserRole.REQUESTER)));

        assertSame(ErrorCode.REQUEST_STATUS_CONFLICT, exception.getErrorCode());
        verify(acceptanceMapper, never()).insert(
                any(AcceptanceEntity.class));
        verifyNoInteractions(statusHistoryMapper);
    }

    @Test
    void deliveryInsertFailurePreventsHistoryInsertion() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.IN_PROGRESS, 4));
        when(requestMapper.compareAndSetStatus(
                REQUEST_ID,
                "IN_PROGRESS",
                "PENDING_ACCEPTANCE",
                4))
                .thenReturn(1);
        when(deliveryMapper.insert(any(DeliveryEntity.class)))
                .thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createDelivery(
                        REQUEST_ID,
                        deliveryCommand(4),
                        loginUser(9L, UserRole.ADMIN)));

        assertSame(ErrorCode.INTERNAL_ERROR, exception.getErrorCode());
        verifyNoInteractions(statusHistoryMapper);
    }

    @Test
    void acceptanceInsertFailurePreventsHistoryInsertion() {
        DeliveryEntity latest = delivery(33L, 2L);
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.PENDING_ACCEPTANCE, 5));
        when(deliveryMapper.selectLatestByRequestId(REQUEST_ID))
                .thenReturn(latest);
        when(acceptanceMapper.countByDeliveryId(33L)).thenReturn(0L);
        when(requestMapper.compareAndSetStatus(
                REQUEST_ID,
                "PENDING_ACCEPTANCE",
                "COMPLETED",
                5))
                .thenReturn(1);
        when(acceptanceMapper.insert(any(AcceptanceEntity.class)))
                .thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createAcceptance(
                        REQUEST_ID,
                        acceptedCommand(5, null),
                        loginUser(1L, UserRole.REQUESTER)));

        assertSame(ErrorCode.INTERNAL_ERROR, exception.getErrorCode());
        verifyNoInteractions(statusHistoryMapper);
    }

    @Test
    void acceptanceRejectsWrongStatusAndStaleVersion() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(
                        request(1L, RequestStatus.IN_PROGRESS, 5),
                        request(1L, RequestStatus.PENDING_ACCEPTANCE, 6));

        BusinessException statusException = assertThrows(
                BusinessException.class,
                () -> service.createAcceptance(
                        REQUEST_ID,
                        acceptedCommand(5, null),
                        loginUser(1L, UserRole.REQUESTER)));
        BusinessException versionException = assertThrows(
                BusinessException.class,
                () -> service.createAcceptance(
                        REQUEST_ID,
                        acceptedCommand(5, null),
                        loginUser(1L, UserRole.REQUESTER)));

        assertSame(ErrorCode.REQUEST_STATUS_CONFLICT,
                statusException.getErrorCode());
        assertSame(ErrorCode.REQUEST_STATUS_CONFLICT,
                versionException.getErrorCode());
        verifyNoInteractions(
                deliveryMapper,
                acceptanceMapper,
                statusHistoryMapper);
    }

    private CreateDeliveryCommand deliveryCommand(int version) {
        return new CreateDeliveryCommand(
                version,
                "完成系统功能并提供测试说明",
                "https://example.edu/delivery/100");
    }

    private CreateAcceptanceCommand acceptedCommand(
            int version,
            String comment) {
        return new CreateAcceptanceCommand(
                version,
                AcceptanceResult.ACCEPTED,
                comment);
    }

    private RequestEntity request(
            Long creatorId,
            RequestStatus status,
            int version) {
        RequestEntity request = new RequestEntity();
        request.setId(REQUEST_ID);
        request.setCreatorId(creatorId);
        request.setStatus(status);
        request.setVersion(version);
        request.setProgress(80);
        return request;
    }

    private RequestMemberEntity member(
            Long userId,
            RequestMemberType type) {
        RequestMemberEntity member = new RequestMemberEntity();
        member.setId(userId + 100);
        member.setRequestId(REQUEST_ID);
        member.setUserId(userId);
        member.setMemberType(type);
        member.setJoinedAt(Instant.parse("2026-08-12T08:00:00Z"));
        return member;
    }

    private DeliveryEntity delivery(Long id, Long submitterId) {
        DeliveryEntity delivery = new DeliveryEntity();
        delivery.setId(id);
        delivery.setRequestId(REQUEST_ID);
        delivery.setSubmitterId(submitterId);
        delivery.setDescription("交付说明完整");
        delivery.setDeliveryUrl("https://example.edu/delivery/100");
        delivery.setCreatedAt(Instant.parse("2026-08-12T08:00:00Z"));
        return delivery;
    }

    private UserEntity user(Long id, String displayName) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setDisplayName(displayName);
        return user;
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

    private void assignIdOnInsert(
            DeliveryMapper mapper,
            Long id) {
        doAnswer(invocation -> {
            DeliveryEntity entity = invocation.getArgument(0);
            entity.setId(id);
            return 1;
        }).when(mapper).insert(any(DeliveryEntity.class));
    }

    private void assignAcceptanceIdOnInsert(Long id) {
        doAnswer(invocation -> {
            AcceptanceEntity entity = invocation.getArgument(0);
            entity.setId(id);
            return 1;
        }).when(acceptanceMapper).insert(any(AcceptanceEntity.class));
    }
}
