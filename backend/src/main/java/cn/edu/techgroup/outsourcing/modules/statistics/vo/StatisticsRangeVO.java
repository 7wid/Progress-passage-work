package cn.edu.techgroup.outsourcing.modules.statistics.vo;

import java.time.LocalDate;

public record StatisticsRangeVO(LocalDate from, LocalDate to, Long categoryId) {}
