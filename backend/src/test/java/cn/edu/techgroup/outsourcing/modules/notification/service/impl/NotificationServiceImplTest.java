package cn.edu.techgroup.outsourcing.modules.notification.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.techgroup.outsourcing.common.api.PageResponse;
import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.notification.dto.NotificationListQuery;
import cn.edu.techgroup.outsourcing.modules.notification.entity.NotificationEntity;
import cn.edu.techgroup.outsourcing.modules.notification.enums.NotificationType;
import cn.edu.techgroup.outsourcing.modules.notification.event.NotificationEvent;
import cn.edu.techgroup.outsourcing.modules.notification.mapper.NotificationMapper;
import cn.edu.techgroup.outsourcing.modules.notification.vo.MarkAllReadResultVO;
import cn.edu.techgroup.outsourcing.modules.notification.vo.NotificationVO;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @BeforeAll
    static void initializeMybatisMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                NotificationEntity.class);
    }

    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private UserMapper userMapper;

    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(notificationMapper, userMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listsOnlyCurrentUsersNotificationsAsPage() {
        NotificationEntity entity = notification(11L, 7L, false);
        when(notificationMapper.selectPage(any(Page.class), any(Wrapper.class)))
                .thenAnswer(invocation -> {
                    Page<NotificationEntity> page = invocation.getArgument(0);
                    page.setRecords(List.of(entity));
                    page.setTotal(1);
                    return page;
                });

        PageResponse<NotificationVO> result = service.list(
                new NotificationListQuery(null, null, true),
                loginUser(7L));

        assertEquals(1, result.page());
        assertEquals(20, result.pageSize());
        assertEquals(1, result.total());
        assertEquals("11", result.items().getFirst().id());
        assertFalse(result.items().getFirst().read());
    }

    @Test
    void countsOnlyCurrentUsersUnreadNotifications() {
        when(notificationMapper.selectCount(any(Wrapper.class))).thenReturn(3L);

        assertEquals(3, service.unreadCount(loginUser(7L)).unreadCount());
    }

    @Test
    void marksOwnUnreadNotificationAndReturnsFreshState() {
        NotificationEntity unread = notification(11L, 7L, false);
        NotificationEntity read = notification(11L, 7L, true);
        read.setReadAt(Instant.parse("2026-08-14T08:00:00Z"));
        when(notificationMapper.selectOne(any(Wrapper.class)))
                .thenReturn(unread, read);
        when(notificationMapper.markRead(anyLong(), anyLong(), any(Instant.class)))
                .thenReturn(1);

        NotificationVO result = service.markRead(11L, loginUser(7L));

        assertTrue(result.read());
        assertEquals(read.getReadAt(), result.readAt());
        verify(notificationMapper).markRead(
                org.mockito.ArgumentMatchers.eq(11L),
                org.mockito.ArgumentMatchers.eq(7L),
                any(Instant.class));
    }

    @Test
    void markingAlreadyReadNotificationIsIdempotent() {
        NotificationEntity read = notification(11L, 7L, true);
        Instant originalReadAt = Instant.parse("2026-08-14T08:00:00Z");
        read.setReadAt(originalReadAt);
        when(notificationMapper.selectOne(any(Wrapper.class))).thenReturn(read);

        NotificationVO result = service.markRead(11L, loginUser(7L));

        assertTrue(result.read());
        assertEquals(originalReadAt, result.readAt());
        verify(notificationMapper, never()).markRead(anyLong(), anyLong(), any());
    }

    @Test
    void hidesForeignOrMissingNotification() {
        when(notificationMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.markRead(11L, loginUser(7L)));

        assertSame(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void marksAllUnreadNotificationsIdempotently() {
        when(notificationMapper.markAllRead(anyLong(), any(Instant.class)))
                .thenReturn(4, 0);

        MarkAllReadResultVO first = service.markAllRead(loginUser(7L));
        MarkAllReadResultVO second = service.markAllRead(loginUser(7L));

        assertEquals(4, first.updatedCount());
        assertEquals(0, second.updatedCount());
    }

    @Test
    void dispatchDeduplicatesRecipientsAndExcludesActor() {
        when(notificationMapper.insert(any(NotificationEntity.class))).thenReturn(1);
        NotificationEvent event = new NotificationEvent(
                NotificationType.ASSIGNMENT_UPDATED,
                100L,
                2L,
                List.of(2L, 3L, 3L, 4L),
                "成员已更新",
                "需求 TR-100 的成员已更新。");

        service.dispatch(event);

        ArgumentCaptor<NotificationEntity> captor =
                ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationMapper, times(2)).insert(captor.capture());
        assertEquals(List.of(3L, 4L), captor.getAllValues().stream()
                .map(NotificationEntity::getRecipientId)
                .toList());
        assertTrue(captor.getAllValues().stream()
                .allMatch(item -> !Boolean.TRUE.equals(item.getRead())));
    }

    @Test
    void resolvesActiveTeamRecipientsDuringPostCommitDispatch() {
        when(userMapper.selectActiveTeamUserIds())
                .thenReturn(List.of(2L, 3L, 3L));
        when(notificationMapper.insert(any(NotificationEntity.class))).thenReturn(1);

        service.dispatch(new NotificationEvent(
                NotificationType.REQUEST_SUBMITTED,
                100L,
                2L,
                List.of(),
                "有新的需求待评估",
                "需求 TR-100 已提交，请及时评估。"));

        ArgumentCaptor<NotificationEntity> captor =
                ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationMapper).insert(captor.capture());
        assertEquals(3L, captor.getValue().getRecipientId());
    }

    @Test
    void resolvesActiveAdministratorsForRejectionConfirmation() {
        when(userMapper.selectActiveAdminIds())
                .thenReturn(List.of(8L, 9L, 9L));
        when(notificationMapper.insert(any(NotificationEntity.class))).thenReturn(1);

        service.dispatch(new NotificationEvent(
                NotificationType.REJECTION_CONFIRMATION_REQUIRED,
                100L,
                8L,
                List.of(),
                "评估驳回待确认",
                "需求 TR-100 的不承接评估需要管理员确认。"));

        ArgumentCaptor<NotificationEntity> captor =
                ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationMapper).insert(captor.capture());
        assertEquals(9L, captor.getValue().getRecipientId());
    }

    @Test
    void dispatchWithNoRecipientsDoesNothing() {
        service.dispatch(new NotificationEvent(
                NotificationType.PROGRESS_UPDATED,
                100L,
                2L,
                List.of(2L),
                "进度已更新",
                "需求 TR-100 发布了公开进度。"));

        verify(notificationMapper, never()).insert(any(NotificationEntity.class));
    }

    @Test
    void rejectsInvalidEventBeforeWriting() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.dispatch(new NotificationEvent(
                        null,
                        100L,
                        2L,
                        List.of(3L),
                        "标题",
                        "内容")));

        assertSame(ErrorCode.INVALID_ARGUMENT, exception.getErrorCode());
        verify(notificationMapper, never()).insert(any(NotificationEntity.class));
    }

    @Test
    void rejectsUnauthenticatedQueries() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.unreadCount(null));

        assertSame(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
    }

    private NotificationEntity notification(
            Long id,
            Long recipientId,
            boolean read) {
        NotificationEntity entity = new NotificationEntity();
        entity.setId(id);
        entity.setRecipientId(recipientId);
        entity.setRequestId(100L);
        entity.setType(NotificationType.REQUEST_SUBMITTED);
        entity.setTitle("新需求");
        entity.setContent("有新的需求待处理。");
        entity.setRead(read);
        entity.setCreatedAt(Instant.parse("2026-08-14T07:00:00Z"));
        return entity;
    }

    private LoginUser loginUser(Long id) {
        return new LoginUser(
                id,
                "member" + id,
                "password",
                "成员" + id,
                UserRole.MEMBER,
                true,
                true);
    }
}
