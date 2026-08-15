package cn.edu.techgroup.outsourcing.modules.statistics.mapper;

import java.time.LocalDate;

public record StatisticsTrendRow(LocalDate statisticDate, Long count) {}
