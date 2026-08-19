package cn.edu.techgroup.outsourcing.modules.statistics.vo;

import java.time.Instant;
import java.util.List;

public record AdminStatisticsDashboardVO(
        StatisticsRangeVO range,
        StatisticsKpiVO kpis,
        List<StatusCountVO> statusDistribution,
        List<CategoryCountVO> categoryDistribution,
        List<DailyRequestCountVO> submissionTrend,
        List<MemberWorkloadVO> memberWorkloads,
        Instant generatedAt) {}
