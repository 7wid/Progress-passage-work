package cn.edu.techgroup.outsourcing.modules.assignment.vo;

import cn.edu.techgroup.outsourcing.modules.assignment.enums.RequestMemberType;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import java.time.Instant;

public record RequestMemberVO(
        String id,
        String userId,
        String displayName,
        UserRole role,
        RequestMemberType memberType,
        Instant joinedAt) {
}