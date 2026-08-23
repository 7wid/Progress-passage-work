package cn.edu.techgroup.outsourcing.modules.assignment.vo;

import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import java.util.List;

public record MemberRecommendationVO(
        String id,
        String account,
        String displayName,
        UserRole role,
        List<String> skills,
        List<String> matchedSkills,
        long activeRequestCount,
        long projectedActiveRequestCount,
        int rank) {
}
