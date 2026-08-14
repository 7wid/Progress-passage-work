package cn.edu.techgroup.outsourcing.modules.notification.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record NotificationListQuery(
        @Min(value = 1, message = "页码不能小于 1")
        Integer page,

        @Min(value = 1, message = "每页数量不能小于 1")
        @Max(value = 100, message = "每页数量不能超过 100")
        Integer pageSize,

        Boolean unreadOnly) {

    public NotificationListQuery {
        page = page == null ? 1 : page;
        pageSize = pageSize == null ? 20 : pageSize;
        unreadOnly = unreadOnly == null ? Boolean.FALSE : unreadOnly;
    }
}
