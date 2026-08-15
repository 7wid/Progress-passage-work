package cn.edu.techgroup.outsourcing.modules.statistics.vo;

import java.time.LocalDate;

public record DailyRequestCountVO(LocalDate date, long count) {}
