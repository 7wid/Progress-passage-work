package cn.edu.techgroup.outsourcing.modules.request.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.assignment.mapper.RequestMemberMapper;
import cn.edu.techgroup.outsourcing.modules.category.entity.CategoryEntity;
import cn.edu.techgroup.outsourcing.modules.category.mapper.CategoryMapper;
import cn.edu.techgroup.outsourcing.modules.notification.event.NotificationEventPublisher;
import cn.edu.techgroup.outsourcing.modules.notification.enums.NotificationType;
import cn.edu.techgroup.outsourcing.modules.progress.entity.StatusHistoryEntity;
import cn.edu.techgroup.outsourcing.modules.progress.mapper.StatusHistoryMapper;
import cn.edu.techgroup.outsourcing.modules.request.dto.CreateRequestCommand;
import cn.edu.techgroup.outsourcing.modules.request.dto.RequestListQuery;
import cn.edu.techgroup.outsourcing.modules.request.entity.RequestEntity;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestSort;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestUrgency;
import cn.edu.techgroup.outsourcing.modules.request.mapper.RequestMapper;
import cn.edu.techgroup.outsourcing.modules.request.service.RequestNumberGenerator;
import cn.edu.techgroup.outsourcing.modules.request.vo.RequestDetailVO;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditActions;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @BeforeAll
    static void initializeMybatisMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                RequestEntity.class);
    }

    @Mock
    private RequestMapper requestMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private StatusHistoryMapper statusHistoryMapper;
    @Mock
    private RequestMemberMapper requestMemberMapper;
    @Mock
    private RequestNumberGenerator requestNumberGenerator;
    @Mock
    private UserMapper userMapper;
    @Mock
    private NotificationEventPublisher notificationEventPublisher;
    @Mock
    private AuditRecorder auditRecorder;

    private RequestServiceImpl requestService;

    @BeforeEach
    void setUp() {
        requestService = new RequestServiceImpl(
                requestMapper,
                categoryMapper,
                statusHistoryMapper,
                requestNumberGenerator,
                requestMemberMapper,
                userMapper,
                notificationEventPublisher,
                auditRecorder);
    }

    @Test
    void submittedRequestPublishesTeamNotificationEvent() {
        CategoryEntity category = new CategoryEntity();
        category.setId(10L);
        category.setEnabled(true);
        when(categoryMapper.selectById(10L)).thenReturn(category);
        when(requestMapper.insert(any(RequestEntity.class)))
                .thenAnswer(invocation -> {
                    RequestEntity entity = invocation.getArgument(0);
                    entity.setId(100L);
                    return 1;
                });
        when(requestNumberGenerator.generate(any(), any()))
                .thenReturn("REQ-20260814-0100");
        when(requestMapper.update(
                isNull(),
                any(Wrapper.class)))
                .thenReturn(1);
        when(statusHistoryMapper.insert(any(StatusHistoryEntity.class))).thenReturn(1);

        var result = requestService.createAndSubmit(
                new CreateRequestCommand(
                        10L,
                        "校园预约系统开发",
                        "这是用于测试需求提交通知的完整项目背景说明。",
                        "需要实现一个可以提交预约、审核预约并查看处理状态的校园预约管理系统。",
                        "提供可以正常使用的前后端系统",
                        LocalDate.now().plusDays(10),
                        RequestUrgency.NORMAL,
                        new BigDecimal("1000.00"),
                        null,
                        null,
                        "requester@example.edu.cn",
                        true),
                loginUser(1L, UserRole.REQUESTER));

        assertEquals("REQ-20260814-0100", result.requestNo());
        verify(notificationEventPublisher).publish(argThat(event ->
                event.type() == NotificationType.REQUEST_SUBMITTED
                        && event.requestId().equals(100L)
                        && event.actorId().equals(1L)
                        && event.recipientIds().isEmpty()));
        verify(auditRecorder).record(
                eq(1L),
                eq(AuditActions.REQUEST_SUBMITTED),
                eq("REQUEST"),
                eq("100"),
                isNull(),
                any());
    }

    @Test
    void rejectsReversedSubmittedDateRange() {
        RequestListQuery query = new RequestListQuery(
                1,
                20,
                null,
                null,
                null,
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 9),
                RequestSort.NEWEST,
                null,
                false,
                false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> requestService.list(query, loginUser(2L, UserRole.MEMBER)));

        assertSame(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
        verifyNoInteractions(
                requestMapper,
                categoryMapper,
                statusHistoryMapper,
                requestMemberMapper,
                userMapper);
    }

    @Test
    void adminCanViewContactInformation() {
        stubDetailDependencies();

        RequestDetailVO detail = requestService.getDetail(
                100L,
                loginUser(2L, UserRole.ADMIN));

        assertEquals("requester@example.edu.cn", detail.contactInfo());
    }

    @Test
    void unrelatedMemberCannotViewContactInformation() {
        stubDetailDependencies();

        RequestDetailVO detail = requestService.getDetail(
                100L,
                loginUser(2L, UserRole.MEMBER));

        assertNull(detail.contactInfo());
    }

    @Test
    void assignedMemberCanViewContactInformation() {
        stubDetailDependencies();
        when(requestMemberMapper.countByRequestIdAndUserId(100L, 2L))
                .thenReturn(1L);

        RequestDetailVO detail = requestService.getDetail(
                100L,
                loginUser(2L, UserRole.MEMBER));

        assertEquals("requester@example.edu.cn", detail.contactInfo());
    }

    private void stubDetailDependencies() {
        RequestEntity request = new RequestEntity();
        request.setId(100L);
        request.setRequestNo("REQ-20260809-0100");
        request.setCreatorId(1L);
        request.setCategoryId(10L);
        request.setTitle("校园网站功能开发");
        request.setBackground("用于测试的需求背景内容");
        request.setDescription("用于测试的详细需求描述");
        request.setExpectedResult("可运行的网站功能");
        request.setExpectedDeadline(LocalDate.of(2026, 9, 1));
        request.setUrgency(RequestUrgency.NORMAL);
        request.setContactInfo("requester@example.edu.cn");
        request.setStatus(RequestStatus.PENDING_REVIEW);
        request.setProgress(0);
        request.setVersion(0);
        request.setSubmittedAt(Instant.parse("2026-08-09T08:00:00Z"));
        request.setCreatedAt(Instant.parse("2026-08-09T08:00:00Z"));
        request.setUpdatedAt(Instant.parse("2026-08-09T08:00:00Z"));

        CategoryEntity category = new CategoryEntity();
        category.setId(10L);
        category.setName("Web 开发");

        UserEntity creator = new UserEntity();
        creator.setId(1L);
        creator.setDisplayName("需求方");

        when(requestMapper.selectOne(any())).thenReturn(request);
        when(categoryMapper.selectById(10L)).thenReturn(category);
        when(userMapper.selectById(1L)).thenReturn(creator);
        when(statusHistoryMapper.selectList(any())).thenReturn(List.of());
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
