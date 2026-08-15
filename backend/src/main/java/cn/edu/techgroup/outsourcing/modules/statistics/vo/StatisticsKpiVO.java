package cn.edu.techgroup.outsourcing.modules.statistics.vo;

import java.math.BigDecimal;

public record StatisticsKpiVO(
        long submittedCount,
        long completedCount,
        BigDecimal completionRate,
        long firstResponseSampleCount,
        BigDecimal averageFirstResponseHours) {}
