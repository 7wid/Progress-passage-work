package cn.edu.techgroup.outsourcing.modules.request.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.assignment.enums.RequestMemberType;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.category.entity.CategoryEntity;
import cn.edu.techgroup.outsourcing.modules.category.mapper.CategoryMapper;
import cn.edu.techgroup.outsourcing.modules.notification.event.NotificationEventPublisher;
import cn.edu.techgroup.outsourcing.modules.notification.enums.NotificationType;
import cn.edu.techgroup.outsourcing.modules.progress.entity.StatusHistoryEntity;
import cn.edu.techgroup.outsourcing.modules.progress.mapper.StatusHistoryMapper;
import cn.edu.techgroup.outsourcing.modules.request.dto.CancelRequestCommand;
import cn.edu.techgroup.outsourcing.modules.request.dto.SaveDraftCommand;
import cn.edu.techgroup.outsourcing.modules.request.dto.SubmitRequestCommand;
import cn.edu.techgroup.outsourcing.modules.request.entity.RequestEntity;
import cn.edu.techgroup.outsourcing.modules.request.entity.RequestRevisionEntity;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestUrgency;
import cn.edu.techgroup.outsourcing.modules.request.mapper.RequestMapper;
import cn.edu.techgroup.outsourcing.modules.request.mapper.RequestRevisionMapper;
import cn.edu.techgroup.outsourcing.modules.request.service.RequestNumberGenerator;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequesterRequestLifecycleServiceImplTest {

    @Mock private RequestMapper requestMapper;
    @Mock private RequestRevisionMapper requestRevisionMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private StatusHistoryMapper statusHistoryMapper;
    @Mock private RequestNumberGenerator requestNumberGenerator;
    @Mock private UserMapper userMapper;
    @Mock private NotificationEventPublisher notificationEventPublisher;
    @Mock private AuditRecorder auditRecorder;

    private RequesterRequestLifecycleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RequesterRequestLifecycleServiceImpl(
                requestMapper,
                requestRevisionMapper,
                categoryMapper,
                statusHistoryMapper,
                requestNumberGenerator,
                userMapper,
                notificationEventPublisher,
                auditRecorder,
                new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void createsIncompleteDraftWithRevisionAndHistory() {
        when(requestMapper.insert(any(RequestEntity.class))).thenAnswer(invocation -> {
            RequestEntity request = invocation.getArgument(0);
            request.setId(100L);
            return 1;
        });
        when(statusHistoryMapper.insert(any(StatusHistoryEntity.class))).thenReturn(1);
        when(requestRevisionMapper.selectMaxRevisionNo(100L)).thenReturn(0);
        when(requestRevisionMapper.insert(any(RequestRevisionEntity.class))).thenReturn(1);

        var result = service.createDraft(
                new SaveDraftCommand(
                        null, null, null, null, null, null, null,
                        null, null, null, null),
                loginUser(1L));

        assertEquals(RequestStatus.DRAFT, result.status());
        assertEquals("100", result.id());
        verify(requestRevisionMapper).insert(
                org.mockito.ArgumentMatchers.<RequestRevisionEntity>argThat(revision ->
                revision.getRevisionNo() == 1
                        && revision.getContentSnapshot().contains("\"status\":\"DRAFT\"")));
        verify(statusHistoryMapper).insert(
                org.mockito.ArgumentMatchers.<StatusHistoryEntity>argThat(history ->
                history.getFromStatus() == null
                        && history.getToStatus() == RequestStatus.DRAFT));
    }

    @Test
    void submitsCompleteDraftWithOptimisticVersion() {
        RequestEntity request = completeRequest();
        when(requestMapper.selectByIdForUpdate(100L)).thenReturn(request);
        CategoryEntity category = new CategoryEntity();
        category.setEnabled(true);
        when(categoryMapper.selectById(10L)).thenReturn(category);
        when(requestNumberGenerator.generate(any(), any())).thenReturn("REQ-20260816-0100");
        when(requestMapper.submitRequesterRequest(
                eq(100L),
                eq("DRAFT"),
                eq(3),
                eq("REQ-20260816-0100"),
                any())).thenReturn(1);
        when(statusHistoryMapper.insert(any(StatusHistoryEntity.class))).thenReturn(1);

        var result = service.submit(
                100L,
                new SubmitRequestCommand(3, true),
                loginUser(1L));

        assertEquals(RequestStatus.PENDING_REVIEW, result.status());
        assertEquals(4, result.version());
        assertEquals("REQ-20260816-0100", result.requestNo());
        verify(notificationEventPublisher).publish(argThat(event ->
                event.type() == NotificationType.REQUEST_SUBMITTED));
    }

    @Test
    void hidesRequestOwnedByAnotherRequester() {
        RequestEntity request = completeRequest();
        request.setCreatorId(9L);
        when(requestMapper.selectByIdForUpdate(100L)).thenReturn(request);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.cancel(
                        100L,
                        new CancelRequestCommand(3, "需求不再需要"),
                        loginUser(1L)));

        assertSame(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        verify(requestMapper, never()).compareAndSetStatus(any(), any(), any(), any());
    }

    @Test
    void cancelsSubmittedRequestAndNotifiesTeam() {
        RequestEntity request = completeRequest();
        request.setStatus(RequestStatus.PENDING_ASSIGNMENT);
        request.setRequestNo("REQ-20260816-0100");
        when(requestMapper.selectByIdForUpdate(100L)).thenReturn(request);
        when(requestMapper.compareAndSetStatus(
                100L, "PENDING_ASSIGNMENT", "CANCELLED", 3)).thenReturn(1);
        when(statusHistoryMapper.insert(any(StatusHistoryEntity.class))).thenReturn(1);
        when(userMapper.selectActiveTeamUserIds()).thenReturn(List.of(2L, 3L));

        var result = service.cancel(
                100L,
                new CancelRequestCommand(3, "需求计划已经取消"),
                loginUser(1L));

        assertEquals(RequestStatus.CANCELLED, result.status());
        verify(notificationEventPublisher).publish(argThat(event ->
                event.type() == NotificationType.REQUEST_CANCELLED
                        && event.recipientIds().equals(List.of(2L, 3L))));
    }

    private RequestEntity completeRequest() {
        RequestEntity request = new RequestEntity();
        request.setId(100L);
        request.setCreatorId(1L);
        request.setCategoryId(10L);
        request.setTitle("校园预约系统开发");
        request.setBackground(
                "这是用于提交测试的完整需求背景说明内容，需要解决当前人工登记容易遗漏和无法追踪的问题。");
        request.setDescription(
                "需要实现一个可以提交预约、审核预约并查看处理状态的校园预约管理系统，"
                        + "至少包含用户提交、管理员审核、状态查询、异常提示以及完整处理记录等业务流程。");
        request.setExpectedResult("提供可以正常使用的前后端系统");
        request.setExpectedDeadline(LocalDate.now().plusDays(10));
        request.setUrgency(RequestUrgency.NORMAL);
        request.setContactInfo("requester@example.edu.cn");
        request.setStatus(RequestStatus.DRAFT);
        request.setProgress(0);
        request.setVersion(3);
        return request;
    }

    private LoginUser loginUser(Long id) {
        return new LoginUser(
                id,
                "requester",
                "password-hash",
                "需求方",
                UserRole.REQUESTER,
                true,
                true);
    }
}
