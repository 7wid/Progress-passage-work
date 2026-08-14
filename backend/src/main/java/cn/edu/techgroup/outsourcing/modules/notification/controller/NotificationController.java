package cn.edu.techgroup.outsourcing.modules.notification.controller;

import cn.edu.techgroup.outsourcing.common.api.ApiResponse;
import cn.edu.techgroup.outsourcing.common.api.PageResponse;
import cn.edu.techgroup.outsourcing.modules.notification.dto.NotificationListQuery;
import cn.edu.techgroup.outsourcing.modules.notification.service.NotificationService;
import cn.edu.techgroup.outsourcing.modules.notification.vo.MarkAllReadResultVO;
import cn.edu.techgroup.outsourcing.modules.notification.vo.NotificationVO;
import cn.edu.techgroup.outsourcing.modules.notification.vo.UnreadCountVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("hasAnyRole('REQUESTER', 'MEMBER', 'ADMIN')")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificationVO>> list(
            @Valid @ModelAttribute NotificationListQuery query,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(notificationService.list(query, loginUser));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountVO> unreadCount(
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(notificationService.unreadCount(loginUser));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<NotificationVO> markRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(
                notificationService.markRead(notificationId, loginUser));
    }

    @PostMapping("/read-all")
    public ApiResponse<MarkAllReadResultVO> markAllRead(
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(notificationService.markAllRead(loginUser));
    }
}
