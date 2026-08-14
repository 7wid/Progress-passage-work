package cn.edu.techgroup.outsourcing.modules.notification.service.impl;

import cn.edu.techgroup.outsourcing.common.api.PageResponse;
import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.notification.dto.NotificationListQuery;
import cn.edu.techgroup.outsourcing.modules.notification.entity.NotificationEntity;
import cn.edu.techgroup.outsourcing.modules.notification.enums.NotificationType;
import cn.edu.techgroup.outsourcing.modules.notification.event.NotificationEvent;
import cn.edu.techgroup.outsourcing.modules.notification.mapper.NotificationMapper;
import cn.edu.techgroup.outsourcing.modules.notification.service.NotificationService;
import cn.edu.techgroup.outsourcing.modules.notification.vo.MarkAllReadResultVO;
import cn.edu.techgroup.outsourcing.modules.notification.vo.NotificationVO;
import cn.edu.techgroup.outsourcing.modules.notification.vo.UnreadCountVO;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_TITLE_LENGTH = 160;
    private static final int MAX_CONTENT_LENGTH = 1000;

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;

    public NotificationServiceImpl(
            NotificationMapper notificationMapper,
            UserMapper userMapper) {
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationVO> list(
            NotificationListQuery query,
            LoginUser viewer) {
        requireViewer(viewer);
        validateQuery(query);

        var wrapper = Wrappers.<NotificationEntity>lambdaQuery()
                .select(
                        NotificationEntity::getId,
                        NotificationEntity::getRequestId,
                        NotificationEntity::getType,
                        NotificationEntity::getTitle,
                        NotificationEntity::getContent,
                        NotificationEntity::getRead,
                        NotificationEntity::getReadAt,
                        NotificationEntity::getCreatedAt)
                .eq(NotificationEntity::getRecipientId, viewer.id())
                .eq(Boolean.TRUE.equals(query.unreadOnly()),
                        NotificationEntity::getRead,
                        false)
                .orderByDesc(NotificationEntity::getCreatedAt)
                .orderByDesc(NotificationEntity::getId);

        Page<NotificationEntity> result = notificationMapper.selectPage(
                new Page<>(query.page(), query.pageSize()),
                wrapper);
        List<NotificationVO> items = result.getRecords().stream()
                .map(NotificationVO::from)
                .toList();
        return PageResponse.of(
                items,
                result.getCurrent(),
                result.getSize(),
                result.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountVO unreadCount(LoginUser viewer) {
        requireViewer(viewer);
        long count = notificationMapper.selectCount(
                Wrappers.<NotificationEntity>lambdaQuery()
                        .eq(NotificationEntity::getRecipientId, viewer.id())
                        .eq(NotificationEntity::getRead, false));
        return new UnreadCountVO(count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationVO markRead(
            Long notificationId,
            LoginUser viewer) {
        requireViewer(viewer);
        NotificationEntity notification = findOwned(notificationId, viewer.id());
        if (!Boolean.TRUE.equals(notification.getRead())) {
            notificationMapper.markRead(notificationId, viewer.id(), Instant.now());
            notification = findOwned(notificationId, viewer.id());
        }
        return NotificationVO.from(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarkAllReadResultVO markAllRead(LoginUser viewer) {
        requireViewer(viewer);
        int updatedCount = notificationMapper.markAllRead(viewer.id(), Instant.now());
        return new MarkAllReadResultVO(updatedCount);
    }

    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            rollbackFor = Exception.class)
    public void dispatch(NotificationEvent event) {
        validateEvent(event);
        Set<Long> recipients = new LinkedHashSet<>(event.recipientIds());
        if (event.type() == NotificationType.REQUEST_SUBMITTED) {
            recipients.addAll(userMapper.selectActiveTeamUserIds());
        } else if (event.type()
                == NotificationType.REJECTION_CONFIRMATION_REQUIRED) {
            recipients.addAll(userMapper.selectActiveAdminIds());
        }
        recipients.remove(null);
        recipients.removeIf(id -> id <= 0 || Objects.equals(id, event.actorId()));
        if (recipients.isEmpty()) {
            return;
        }

        Instant createdAt = Instant.now();
        for (Long recipientId : recipients) {
            NotificationEntity entity = new NotificationEntity();
            entity.setRecipientId(recipientId);
            entity.setRequestId(event.requestId());
            entity.setType(event.type());
            entity.setTitle(event.title().trim());
            entity.setContent(event.content().trim());
            entity.setRead(false);
            entity.setCreatedAt(createdAt);
            if (notificationMapper.insert(entity) != 1) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }
        }
    }

    private NotificationEntity findOwned(Long notificationId, Long recipientId) {
        if (notificationId == null || notificationId <= 0) {
            throw hiddenNotification();
        }
        NotificationEntity notification = notificationMapper.selectOne(
                Wrappers.<NotificationEntity>lambdaQuery()
                        .eq(NotificationEntity::getId, notificationId)
                        .eq(NotificationEntity::getRecipientId, recipientId));
        if (notification == null) {
            throw hiddenNotification();
        }
        return notification;
    }

    private void validateQuery(NotificationListQuery query) {
        if (query == null
                || query.page() == null
                || query.pageSize() == null
                || query.page() < 1
                || query.pageSize() < 1
                || query.pageSize() > 100) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "分页参数不正确");
        }
    }

    private void validateEvent(NotificationEvent event) {
        if (event == null
                || event.type() == null
                || !StringUtils.hasText(event.title())
                || event.title().trim().length() > MAX_TITLE_LENGTH
                || !StringUtils.hasText(event.content())
                || event.content().trim().length() > MAX_CONTENT_LENGTH
                || (event.requestId() != null && event.requestId() <= 0)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "通知事件参数不正确");
        }
    }

    private void requireViewer(LoginUser viewer) {
        if (viewer == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private BusinessException hiddenNotification() {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "通知不存在");
    }
}
