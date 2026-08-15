package cn.edu.techgroup.outsourcing.modules.statistics.dto;

import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record AdminStatisticsQuery(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @Positive(message = "分类 ID 必须为正数") Long categoryId) {}
