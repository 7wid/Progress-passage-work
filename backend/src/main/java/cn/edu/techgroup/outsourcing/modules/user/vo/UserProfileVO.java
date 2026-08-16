package cn.edu.techgroup.outsourcing.modules.user.vo;

import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;

public record UserProfileVO(
        String id,
        String account,
        String displayName,
        String email,
        String phone,
        String department,
        UserRole role) {

    public static UserProfileVO from(UserEntity user) {
        return new UserProfileVO(
                user.getId().toString(),
                user.getAccount(),
                user.getDisplayName(),
                user.getEmail(),
                user.getPhone(),
                user.getDepartment(),
                user.getRole());
    }
}
