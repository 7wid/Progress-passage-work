package cn.edu.techgroup.outsourcing.modules.notification.event;

import cn.edu.techgroup.outsourcing.modules.notification.enums.NotificationType;
import java.util.List;

public final class NotificationEvents {

    private NotificationEvents() {}

    public static NotificationEvent requestSubmitted(
            Long requestId,
            String requestNo,
            Long actorId,
            List<Long> recipients) {
        return event(
                NotificationType.REQUEST_SUBMITTED,
                requestId,
                actorId,
                recipients,
                "有新的需求待评估",
                label(requestId, requestNo) + " 已提交，请及时评估。");
    }

    public static NotificationEvent informationRequired(
            Long requestId,
            String requestNo,
            Long actorId,
            Long requesterId) {
        return event(
                NotificationType.INFO_REQUIRED,
                requestId,
                actorId,
                List.of(requesterId),
                "需求需要补充资料",
                label(requestId, requestNo) + " 需要补充资料，请查看评估说明。");
    }

    public static NotificationEvent evaluationCompleted(
            Long requestId,
            String requestNo,
            Long actorId,
            Long requesterId,
            String publicResult) {
        return event(
                NotificationType.EVALUATION_COMPLETED,
                requestId,
                actorId,
                List.of(requesterId),
                "需求评估已完成",
                label(requestId, requestNo) + " 的评估结果为：" + publicResult + "。");
    }

    public static NotificationEvent rejectionConfirmationRequired(
            Long requestId,
            String requestNo,
            Long actorId,
            List<Long> adminIds) {
        return event(
                NotificationType.REJECTION_CONFIRMATION_REQUIRED,
                requestId,
                actorId,
                adminIds,
                "评估驳回待确认",
                label(requestId, requestNo) + " 的不承接评估需要管理员确认。");
    }

    public static NotificationEvent assignmentUpdated(
            Long requestId,
            String requestNo,
            Long actorId,
            List<Long> recipients) {
        return event(
                NotificationType.ASSIGNMENT_UPDATED,
                requestId,
                actorId,
                recipients,
                "需求成员已更新",
                label(requestId, requestNo) + " 的负责人和参与成员已更新。");
    }

    public static NotificationEvent progressUpdated(
            Long requestId,
            String requestNo,
            Long actorId,
            Long requesterId) {
        return event(
                NotificationType.PROGRESS_UPDATED,
                requestId,
                actorId,
                List.of(requesterId),
                "需求进度已更新",
                label(requestId, requestNo) + " 发布了新的公开进度。");
    }

    public static NotificationEvent deliverySubmitted(
            Long requestId,
            String requestNo,
            Long actorId,
            Long requesterId) {
        return event(
                NotificationType.DELIVERY_SUBMITTED,
                requestId,
                actorId,
                List.of(requesterId),
                "需求已提交交付",
                label(requestId, requestNo) + " 已提交交付，请及时验收。");
    }

    public static NotificationEvent acceptanceCompleted(
            Long requestId,
            String requestNo,
            Long actorId,
            List<Long> recipients,
            boolean accepted) {
        return event(
                NotificationType.ACCEPTANCE_COMPLETED,
                requestId,
                actorId,
                recipients,
                accepted ? "交付验收已通过" : "交付验收已退回",
                label(requestId, requestNo)
                        + (accepted ? " 已通过验收。" : " 的交付已退回，请查看验收意见。"));
    }

    public static NotificationEvent adminRequestUpdated(
            Long requestId, String requestNo, Long actorId,
            List<Long> recipients, boolean reopened) {
        return event(NotificationType.ADMIN_REQUEST_UPDATED, requestId, actorId, recipients,
                reopened ? "需求已由管理员重新开启" : "需求已由管理员取消",
                label(requestId, requestNo) + (reopened ? " 已重新开启，请查看最新状态。" : " 已取消，请查看处理记录。"));
    }

    private static NotificationEvent event(
            NotificationType type,
            Long requestId,
            Long actorId,
            List<Long> recipients,
            String title,
            String content) {
        return new NotificationEvent(
                type,
                requestId,
                actorId,
                recipients,
                title,
                content);
    }

    private static String label(Long requestId, String requestNo) {
        if (requestNo != null && !requestNo.isBlank()) {
            return "需求 " + requestNo;
        }
        return "需求 #" + requestId;
    }
}
