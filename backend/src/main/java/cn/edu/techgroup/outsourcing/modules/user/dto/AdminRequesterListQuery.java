package cn.edu.techgroup.outsourcing.modules.user.dto;

import cn.edu.techgroup.outsourcing.modules.user.enums.UserStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AdminRequesterListQuery(
        @Min(1) Integer page,
        @Min(1) @Max(100) Integer pageSize,
        @Size(max = 80) String keyword,
        UserStatus status) {

    public AdminRequesterListQuery {
        page = page == null ? 1 : page;
        pageSize = pageSize == null ? 20 : pageSize;
        keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
    }
}
