package cn.edu.techgroup.outsourcing.modules.user.vo;

import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserStatus;
import java.time.Instant;
import java.util.List;

public record AdminMemberVO(
        String id,
        String account,
        String displayName,
        String email,
        String phone,
        String department,
        UserRole role,
        UserStatus status,
        List<SkillTagVO> skills,
        long activeOwnerRequestCount,
        Instant createdAt,
        Instant updatedAt) {}
