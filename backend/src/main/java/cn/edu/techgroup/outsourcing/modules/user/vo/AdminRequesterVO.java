package cn.edu.techgroup.outsourcing.modules.user.vo;

import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserStatus;
import java.time.Instant;

public record AdminRequesterVO(
        String id,
        String account,
        String displayName,
        String email,
        String phone,
        String department,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminRequesterVO from(UserEntity user) {
        return new AdminRequesterVO(
                user.getId().toString(), user.getAccount(), user.getDisplayName(),
                user.getEmail(), user.getPhone(), user.getDepartment(),
                user.getStatus(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
