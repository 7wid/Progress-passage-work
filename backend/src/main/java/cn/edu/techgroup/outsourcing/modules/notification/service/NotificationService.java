package cn.edu.techgroup.outsourcing.modules.notification.service;

import cn.edu.techgroup.outsourcing.common.api.PageResponse;
import cn.edu.techgroup.outsourcing.modules.notification.dto.NotificationListQuery;
import cn.edu.techgroup.outsourcing.modules.notification.event.NotificationEvent;
import cn.edu.techgroup.outsourcing.modules.notification.vo.MarkAllReadResultVO;
import cn.edu.techgroup.outsourcing.modules.notification.vo.NotificationVO;
import cn.edu.techgroup.outsourcing.modules.notification.vo.UnreadCountVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;

public interface NotificationService {

    PageResponse<NotificationVO> list(
            NotificationListQuery query,
            LoginUser viewer);

    UnreadCountVO unreadCount(LoginUser viewer);

    NotificationVO markRead(
            Long notificationId,
            LoginUser viewer);

    MarkAllReadResultVO markAllRead(LoginUser viewer);

    void dispatch(NotificationEvent event);
}
