package cn.edu.techgroup.outsourcing.modules.statistics.vo;

public record MemberWorkloadVO(
        String memberId,
        String memberAccount,
        String memberName,
        long activeCount,
        long inProgressCount,
        long pendingAcceptanceCount) {}
