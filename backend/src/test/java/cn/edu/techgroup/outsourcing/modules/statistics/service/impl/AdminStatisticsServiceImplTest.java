package cn.edu.techgroup.outsourcing.modules.statistics.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.statistics.dto.AdminStatisticsQuery;
import cn.edu.techgroup.outsourcing.modules.statistics.mapper.StatisticsCategoryRow;
import cn.edu.techgroup.outsourcing.modules.statistics.mapper.StatisticsMapper;
import cn.edu.techgroup.outsourcing.modules.statistics.mapper.StatisticsStatusRow;
import cn.edu.techgroup.outsourcing.modules.statistics.mapper.StatisticsSummaryRow;
import cn.edu.techgroup.outsourcing.modules.statistics.mapper.StatisticsTrendRow;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminStatisticsServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-15T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private StatisticsMapper statisticsMapper;

    private AdminStatisticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminStatisticsServiceImpl(statisticsMapper, CLOCK);
    }

    @Test
    void buildsExplainableDashboardAndFillsMissingDaysAndStatuses() {
        when(statisticsMapper.selectSummary(any(), any(), isNull()))
                .thenReturn(new StatisticsSummaryRow(4L, 1L, 2L, new BigDecimal("6.50")));
        when(statisticsMapper.selectStatusDistribution(any(), any(), isNull()))
                .thenReturn(List.of(
                        new StatisticsStatusRow("PENDING_REVIEW", 3L),
                        new StatisticsStatusRow("COMPLETED", 1L)));
        when(statisticsMapper.selectCategoryDistribution(any(), any(), isNull()))
                .thenReturn(List.of(new StatisticsCategoryRow(8L, "网站开发", 4L)));
        when(statisticsMapper.selectSubmissionTrend(any(), any(), isNull()))
                .thenReturn(List.of(
                        new StatisticsTrendRow(LocalDate.of(2026, 8, 1), 1L),
                        new StatisticsTrendRow(LocalDate.of(2026, 8, 3), 3L)));

        var result = service.getDashboard(
                new AdminStatisticsQuery(
                        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3), null),
                user(UserRole.ADMIN));

        assertEquals(4, result.kpis().submittedCount());
        assertEquals(new BigDecimal("25.00"), result.kpis().completionRate());
        assertEquals(new BigDecimal("6.50"), result.kpis().averageFirstResponseHours());
        assertEquals(8, result.statusDistribution().size());
        assertEquals(0, result.statusDistribution().stream()
                .filter(item -> item.status().name().equals("IN_PROGRESS"))
                .findFirst().orElseThrow().count());
        assertEquals(List.of(1L, 0L, 3L), result.submissionTrend().stream()
                .map(item -> item.count()).toList());
        assertEquals("8", result.categoryDistribution().getFirst().categoryId());

        verify(statisticsMapper).selectSummary(
                Instant.parse("2026-07-31T16:00:00Z"),
                Instant.parse("2026-08-03T16:00:00Z"),
                null);
    }

    @Test
    void defaultsToCurrentBusinessMonthAndHandlesEmptyData() {
        when(statisticsMapper.selectSummary(any(), any(), isNull())).thenReturn(null);
        when(statisticsMapper.selectStatusDistribution(any(), any(), isNull())).thenReturn(List.of());
        when(statisticsMapper.selectCategoryDistribution(any(), any(), isNull())).thenReturn(List.of());
        when(statisticsMapper.selectSubmissionTrend(any(), any(), isNull())).thenReturn(List.of());

        var result = service.getDashboard(new AdminStatisticsQuery(null, null, null), user(UserRole.ADMIN));

        assertEquals(LocalDate.of(2026, 8, 1), result.range().from());
        assertEquals(LocalDate.of(2026, 8, 15), result.range().to());
        assertEquals(new BigDecimal("0.00"), result.kpis().completionRate());
        assertEquals(15, result.submissionTrend().size());
    }

    @Test
    void rejectsInvalidOrExcessiveRangesBeforeQueryingDatabase() {
        BusinessException reversed = assertThrows(
                BusinessException.class,
                () -> service.getDashboard(
                        new AdminStatisticsQuery(
                                LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 1), null),
                        user(UserRole.ADMIN)));
        assertEquals(ErrorCode.INVALID_ARGUMENT, reversed.getErrorCode());

        BusinessException excessive = assertThrows(
                BusinessException.class,
                () -> service.getDashboard(
                        new AdminStatisticsQuery(
                                LocalDate.of(2025, 8, 14), LocalDate.of(2026, 8, 15), null),
                        user(UserRole.ADMIN)));
        assertTrue(excessive.getMessage().contains("366"));
        verifyNoInteractions(statisticsMapper);
    }

    @Test
    void nonAdminIsRejectedBeforeDatabase() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getDashboard(
                        new AdminStatisticsQuery(null, null, null), user(UserRole.MEMBER)));
        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
        verifyNoInteractions(statisticsMapper);
    }

    private LoginUser user(UserRole role) {
        return new LoginUser(9L, "admin", "x", "管理员", role, true, true);
    }
}
