package cn.edu.techgroup.outsourcing.modules.statistics.mapper;

import java.math.BigDecimal;

public record StatisticsSummaryRow(
        Long submittedCount,
        Long completedCount,
        Long firstResponseSampleCount,
        BigDecimal averageFirstResponseHours) {}
