package cn.edu.techgroup.outsourcing.modules.statistics.service.impl;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import cn.edu.techgroup.outsourcing.modules.statistics.dto.AdminStatisticsQuery;
import cn.edu.techgroup.outsourcing.modules.statistics.mapper.StatisticsCategoryRow;
import cn.edu.techgroup.outsourcing.modules.statistics.mapper.StatisticsMapper;
import cn.edu.techgroup.outsourcing.modules.statistics.mapper.StatisticsStatusRow;
import cn.edu.techgroup.outsourcing.modules.statistics.mapper.StatisticsSummaryRow;
import cn.edu.techgroup.outsourcing.modules.statistics.mapper.StatisticsTrendRow;
import cn.edu.techgroup.outsourcing.modules.statistics.service.AdminStatisticsService;
import cn.edu.techgroup.outsourcing.modules.statistics.vo.AdminStatisticsDashboardVO;
import cn.edu.techgroup.outsourcing.modules.statistics.vo.CategoryCountVO;
import cn.edu.techgroup.outsourcing.modules.statistics.vo.DailyRequestCountVO;
import cn.edu.techgroup.outsourcing.modules.statistics.vo.StatisticsKpiVO;
import cn.edu.techgroup.outsourcing.modules.statistics.vo.StatisticsRangeVO;
import cn.edu.techgroup.outsourcing.modules.statistics.vo.StatusCountVO;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminStatisticsServiceImpl implements AdminStatisticsService {

    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final long MAX_RANGE_DAYS = 365;

    private final StatisticsMapper statisticsMapper;
    private final Clock clock;

    @Autowired
    public AdminStatisticsServiceImpl(StatisticsMapper statisticsMapper) {
        this(statisticsMapper, Clock.systemUTC());
    }

    AdminStatisticsServiceImpl(StatisticsMapper statisticsMapper, Clock clock) {
        this.statisticsMapper = statisticsMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStatisticsDashboardVO getDashboard(
            AdminStatisticsQuery query,
            LoginUser operator) {
        requireAdmin(operator);
        ResolvedRange range = resolveRange(query);
        Instant fromInclusive = range.from().atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant toExclusive = range.to().plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
        Long categoryId = query == null ? null : query.categoryId();

        StatisticsSummaryRow summary = statisticsMapper.selectSummary(
                fromInclusive, toExclusive, categoryId);
        List<StatisticsStatusRow> statusRows = statisticsMapper.selectStatusDistribution(
                fromInclusive, toExclusive, categoryId);
        List<StatisticsCategoryRow> categoryRows = statisticsMapper.selectCategoryDistribution(
                fromInclusive, toExclusive, categoryId);
        List<StatisticsTrendRow> trendRows = statisticsMapper.selectSubmissionTrend(
                fromInclusive, toExclusive, categoryId);

        StatisticsSummaryRow safeSummary = summary == null
                ? new StatisticsSummaryRow(0L, 0L, 0L, null)
                : summary;
        long submittedCount = value(safeSummary.submittedCount());
        long completedCount = value(safeSummary.completedCount());
        BigDecimal completionRate = submittedCount == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(completedCount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(submittedCount), 2, RoundingMode.HALF_UP);

        return new AdminStatisticsDashboardVO(
                new StatisticsRangeVO(range.from(), range.to(), categoryId),
                new StatisticsKpiVO(
                        submittedCount,
                        completedCount,
                        completionRate,
                        value(safeSummary.firstResponseSampleCount()),
                        safeSummary.averageFirstResponseHours()),
                buildStatusDistribution(statusRows),
                buildCategoryDistribution(categoryRows),
                buildTrend(range, trendRows),
                Instant.now(clock).truncatedTo(ChronoUnit.MILLIS));
    }

    private ResolvedRange resolveRange(AdminStatisticsQuery query) {
        LocalDate today = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        LocalDate from = query == null || query.from() == null
                ? today.withDayOfMonth(1)
                : query.from();
        LocalDate to = query == null || query.to() == null ? today : query.to();
        Long categoryId = query == null ? null : query.categoryId();

        if (categoryId != null && categoryId <= 0) {
            throw invalidArgument("分类 ID 必须为正数");
        }
        if (from.isAfter(to)) {
            throw invalidArgument("开始日期不能晚于结束日期");
        }
        if (to.isAfter(today)) {
            throw invalidArgument("统计结束日期不能晚于今天");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw invalidArgument("统计时间范围不能超过 366 天");
        }
        return new ResolvedRange(from, to);
    }

    private List<StatusCountVO> buildStatusDistribution(List<StatisticsStatusRow> rows) {
        Map<RequestStatus, Long> counts = new EnumMap<>(RequestStatus.class);
        if (rows != null) {
            for (StatisticsStatusRow row : rows) {
                if (row == null || row.status() == null) {
                    continue;
                }
                try {
                    counts.put(RequestStatus.valueOf(row.status()), value(row.count()));
                } catch (IllegalArgumentException ignored) {
                    // Database constraints and application enums should stay aligned.
                }
            }
        }
        return Arrays.stream(RequestStatus.values())
                .filter(status -> status != RequestStatus.DRAFT)
                .map(status -> new StatusCountVO(status, counts.getOrDefault(status, 0L)))
                .toList();
    }

    private List<CategoryCountVO> buildCategoryDistribution(List<StatisticsCategoryRow> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
                .filter(row -> row != null && row.categoryId() != null)
                .map(row -> new CategoryCountVO(
                        row.categoryId().toString(),
                        row.categoryName(),
                        value(row.count())))
                .toList();
    }

    private List<DailyRequestCountVO> buildTrend(
            ResolvedRange range,
            List<StatisticsTrendRow> rows) {
        Map<LocalDate, Long> counts = new HashMap<>();
        if (rows != null) {
            rows.stream()
                    .filter(row -> row != null && row.statisticDate() != null)
                    .forEach(row -> counts.put(row.statisticDate(), value(row.count())));
        }
        return range.from()
                .datesUntil(range.to().plusDays(1))
                .map(date -> new DailyRequestCountVO(date, counts.getOrDefault(date, 0L)))
                .toList();
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private void requireAdmin(LoginUser operator) {
        if (operator == null || operator.role() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private BusinessException invalidArgument(String message) {
        return new BusinessException(ErrorCode.INVALID_ARGUMENT, message);
    }

    private record ResolvedRange(LocalDate from, LocalDate to) {}
}
