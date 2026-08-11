package cn.edu.techgroup.outsourcing.modules.assignment.vo;

import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;

public record MemberOptionVO(
        String id,
        String account,
        String displayName,
        UserRole role) {
}
