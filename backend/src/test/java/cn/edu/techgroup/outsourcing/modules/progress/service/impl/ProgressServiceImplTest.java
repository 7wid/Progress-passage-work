package cn.edu.techgroup.outsourcing.modules.progress.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
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
import cn.edu.techgroup.outsourcing.modules.progress.dto.CreateProgressCommand;
import cn.edu.techgroup.outsourcing.modules.progress.entity.ProgressLogEntity;
import cn.edu.techgroup.outsourcing.modules.progress.mapper.ProgressLogMapper;
import cn.edu.techgroup.outsourcing.modules.progress.vo.CreatedProgressResultVO;
import cn.edu.techgroup.outsourcing.modules.progress.vo.RequestProgressSnapshotVO;
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
class ProgressServiceImplTest {

    private static final Long REQUEST_ID = 100L;

    @Mock
    private RequestMapper requestMapper;
    @Mock
    private RequestMemberMapper requestMemberMapper;
    @Mock
    private ProgressLogMapper progressLogMapper;
    @Mock
    private UserMapper userMapper;

    private ProgressServiceImpl progressService;

    @BeforeEach
    void setUp() {
        progressService = new ProgressServiceImpl(
                requestMapper,
                requestMemberMapper,
                progressLogMapper,
                userMapper);
    }

    @Test
    void requesterReadsOnlyPublicLogsAndCannotUpdate() {
        ProgressLogEntity log = progressLog(11L, true);
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.IN_PROGRESS, 3, 40));
        when(progressLogMapper.selectByRequestId(REQUEST_ID, false))
                .thenReturn(List.of(log));
        when(userMapper.selectAssignmentUsersByIds(anyCollection()))
                .thenReturn(List.of(user(2L, "负责人")));

        RequestProgressSnapshotVO result = progressService.get(
                REQUEST_ID,
                loginUser(1L, UserRole.REQUESTER));

        assertEquals(40, result.currentProgress());
        assertFalse(result.canUpdateProgress());
        assertEquals(1, result.logs().size());
        assertEquals("负责人", result.logs().getFirst().authorName());
        verifyNoInteractions(requestMemberMapper);
    }

    @Test
    void ownerReadsInternalLogsAndCanUpdate() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.IN_PROGRESS, 3, 40));
        when(requestMemberMapper.selectByRequestId(REQUEST_ID))
                .thenReturn(List.of(member(2L, RequestMemberType.OWNER)));
        when(progressLogMapper.selectByRequestId(REQUEST_ID, true))
                .thenReturn(List.of(progressLog(11L, false)));
        when(userMapper.selectAssignmentUsersByIds(anyCollection()))
                .thenReturn(List.of(user(2L, "负责人")));

        RequestProgressSnapshotVO result = progressService.get(
                REQUEST_ID,
                loginUser(2L, UserRole.MEMBER));

        assertTrue(result.canUpdateProgress());
        assertFalse(result.logs().getFirst().visibleToRequester());
    }

    @Test
    void participantReadsInternalLogsButCannotUpdate() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.IN_PROGRESS, 3, 40));
        when(requestMemberMapper.selectByRequestId(REQUEST_ID))
                .thenReturn(List.of(member(3L, RequestMemberType.PARTICIPANT)));
        when(progressLogMapper.selectByRequestId(REQUEST_ID, true))
                .thenReturn(List.of());

        RequestProgressSnapshotVO result = progressService.get(
                REQUEST_ID,
                loginUser(3L, UserRole.MEMBER));

        assertFalse(result.canUpdateProgress());
    }

    @Test
    void unassignedMemberReadsInternalLogsButCannotUpdate() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.IN_PROGRESS, 3, 40));
        when(requestMemberMapper.selectByRequestId(REQUEST_ID))
                .thenReturn(List.of());
        when(progressLogMapper.selectByRequestId(REQUEST_ID, true))
                .thenReturn(List.of(progressLog(11L, false)));
        when(userMapper.selectAssignmentUsersByIds(anyCollection()))
                .thenReturn(List.of(user(2L, "负责人")));

        RequestProgressSnapshotVO result = progressService.get(
                REQUEST_ID,
                loginUser(8L, UserRole.MEMBER));

        assertFalse(result.canUpdateProgress());
        assertFalse(result.logs().getFirst().visibleToRequester());
    }

    @Test
    void inProgressRequestWithoutRecentLogNeedsFollowUp() {
        RequestEntity request = request(
                1L,
                RequestStatus.IN_PROGRESS,
                3,
                40);
        request.setUpdatedAt(Instant.parse("2026-07-01T08:00:00Z"));
        when(requestMapper.selectById(REQUEST_ID)).thenReturn(request);
        when(progressLogMapper.selectByRequestId(REQUEST_ID, false))
                .thenReturn(List.of());

        RequestProgressSnapshotVO result = progressService.get(
                REQUEST_ID,
                loginUser(1L, UserRole.REQUESTER));

        assertTrue(result.needsFollowUp());
        assertNull(result.lastProgressAt());
    }

    @Test
    void requesterCannotReadAnotherUsersRequest() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(9L, RequestStatus.IN_PROGRESS, 3, 40));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> progressService.get(
                        REQUEST_ID,
                        loginUser(1L, UserRole.REQUESTER)));

        assertSame(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(progressLogMapper, requestMemberMapper, userMapper);
    }

    @Test
    void ownerCreatesProgressWithCompareAndSet() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.IN_PROGRESS, 4, 40));
        when(requestMemberMapper.selectByRequestId(REQUEST_ID))
                .thenReturn(List.of(member(2L, RequestMemberType.OWNER)));
        when(requestMapper.compareAndSetProgress(
                REQUEST_ID,
                "IN_PROGRESS",
                4,
                65))
                .thenReturn(1);
        doAnswer(invocation -> {
            ProgressLogEntity entity = invocation.getArgument(0);
            entity.setId(21L);
            return 1;
        }).when(progressLogMapper).insert(any(ProgressLogEntity.class));

        CreatedProgressResultVO result = progressService.create(
                REQUEST_ID,
                command(4, 65, true),
                loginUser(2L, UserRole.MEMBER));

        assertEquals(65, result.currentProgress());
        assertEquals(5, result.requestVersion());
        assertEquals("21", result.log().id());
        assertEquals("测试用户", result.log().authorName());
    }

    @Test
    void adminCanPublishOneHundredWithoutChangingStatus() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.IN_PROGRESS, 4, 90));
        when(requestMapper.compareAndSetProgress(
                REQUEST_ID,
                "IN_PROGRESS",
                4,
                100))
                .thenReturn(1);
        doAnswer(invocation -> {
            ProgressLogEntity entity = invocation.getArgument(0);
            entity.setId(22L);
            return 1;
        }).when(progressLogMapper).insert(any(ProgressLogEntity.class));

        CreatedProgressResultVO result = progressService.create(
                REQUEST_ID,
                command(4, 100, true),
                loginUser(9L, UserRole.ADMIN));

        assertEquals(100, result.currentProgress());
        verifyNoInteractions(requestMemberMapper);
        verify(requestMapper).compareAndSetProgress(
                REQUEST_ID,
                "IN_PROGRESS",
                4,
                100);
    }

    @Test
    void participantCannotCreateProgress() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.IN_PROGRESS, 4, 40));
        when(requestMemberMapper.selectByRequestId(REQUEST_ID))
                .thenReturn(List.of(member(3L, RequestMemberType.PARTICIPANT)));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> progressService.create(
                        REQUEST_ID,
                        command(4, 50, true),
                        loginUser(3L, UserRole.MEMBER)));

        assertSame(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
        verify(requestMapper, never()).compareAndSetProgress(
                any(), any(), any(), any());
        verifyNoInteractions(progressLogMapper);
    }

    @Test
    void nonInProgressRequestReturnsConflict() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.PENDING_ASSIGNMENT, 4, 0));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> progressService.create(
                        REQUEST_ID,
                        command(4, 10, true),
                        loginUser(9L, UserRole.ADMIN)));

        assertSame(ErrorCode.REQUEST_STATUS_CONFLICT, exception.getErrorCode());
        verifyNoInteractions(requestMemberMapper, progressLogMapper);
    }

    @Test
    void staleVersionReturnsConflictWithoutLog() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.IN_PROGRESS, 5, 40));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> progressService.create(
                        REQUEST_ID,
                        command(4, 50, true),
                        loginUser(9L, UserRole.ADMIN)));

        assertSame(ErrorCode.REQUEST_STATUS_CONFLICT, exception.getErrorCode());
        verifyNoInteractions(requestMemberMapper, progressLogMapper);
    }

    @Test
    void compareAndSetFailureDoesNotInsertLog() {
        when(requestMapper.selectById(REQUEST_ID))
                .thenReturn(request(1L, RequestStatus.IN_PROGRESS, 4, 40));
        when(requestMapper.compareAndSetProgress(
                REQUEST_ID,
                "IN_PROGRESS",
                4,
                50))
                .thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> progressService.create(
                        REQUEST_ID,
                        command(4, 50, false),
                        loginUser(9L, UserRole.ADMIN)));

        assertSame(ErrorCode.REQUEST_STATUS_CONFLICT, exception.getErrorCode());
        verifyNoInteractions(progressLogMapper);
    }

    @Test
    void blankContentIsRejectedBeforeDatabaseAccess() {
        CreateProgressCommand command = new CreateProgressCommand(
                0,
                10,
                "   ",
                null,
                null,
                true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> progressService.create(
                        REQUEST_ID,
                        command,
                        loginUser(9L, UserRole.ADMIN)));

        assertSame(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
        verifyNoInteractions(
                requestMapper,
                requestMemberMapper,
                progressLogMapper,
                userMapper);
    }

    private CreateProgressCommand command(
            int requestVersion,
            int progress,
            boolean visibleToRequester) {
        return new CreateProgressCommand(
                requestVersion,
                progress,
                "完成了核心功能联调",
                "继续处理边界测试",
                Instant.now().plusSeconds(3600),
                visibleToRequester);
    }

    private RequestEntity request(
            Long creatorId,
            RequestStatus status,
            int version,
            int progress) {
        RequestEntity request = new RequestEntity();
        request.setId(REQUEST_ID);
        request.setCreatorId(creatorId);
        request.setStatus(status);
        request.setVersion(version);
        request.setProgress(progress);
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
        member.setJoinedAt(Instant.parse("2026-08-11T08:00:00Z"));
        return member;
    }

    private ProgressLogEntity progressLog(
            Long id,
            boolean visibleToRequester) {
        ProgressLogEntity log = new ProgressLogEntity();
        log.setId(id);
        log.setRequestId(REQUEST_ID);
        log.setAuthorId(2L);
        log.setProgress(40);
        log.setContent("完成了需求分析和接口设计");
        log.setVisibleToRequester(visibleToRequester);
        log.setCreatedAt(Instant.parse("2026-08-11T08:00:00Z"));
        return log;
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
}
