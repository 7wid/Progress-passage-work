package cn.edu.techgroup.outsourcing.modules.statistics.mapper;

public record StatisticsMemberWorkloadRow(
        Long memberId,
        String memberAccount,
        String memberName,
        Long activeCount,
        Long inProgressCount,
        Long pendingAcceptanceCount) {}
