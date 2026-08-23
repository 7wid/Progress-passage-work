package cn.edu.techgroup.outsourcing.modules.assignment.vo;

import java.util.List;

public record MemberRecommendationResultVO(
        String requiredSkills,
        List<MemberRecommendationVO> members) {
}
