package cn.edu.techgroup.outsourcing.modules.statistics.mapper;

import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StatisticsMapper {

    @Select("""
            <script>
            SELECT COUNT(*) AS submitted_count,
                   COALESCE(SUM(tr.status = 'COMPLETED'), 0) AS completed_count,
                   COUNT(first_response.created_at) AS first_response_sample_count,
                   ROUND(AVG(
                       CASE WHEN first_response.created_at IS NOT NULL
                            THEN TIMESTAMPDIFF(
                                SECOND,
                                tr.submitted_at,
                                first_response.created_at
                            ) / 3600.0
                       END
                   ), 2) AS average_first_response_hours
            FROM tech_request tr
            LEFT JOIN evaluation first_response
              ON first_response.request_id = tr.id
             AND first_response.version = 1
            WHERE tr.submitted_at &gt;= #{fromInclusive}
              AND tr.submitted_at &lt; #{toExclusive}
            <if test="categoryId != null">
              AND tr.category_id = #{categoryId}
            </if>
            </script>
            """)
    StatisticsSummaryRow selectSummary(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            @Param("categoryId") Long categoryId);

    @Select("""
            <script>
            SELECT tr.status AS status, COUNT(*) AS count
            FROM tech_request tr
            WHERE tr.submitted_at &gt;= #{fromInclusive}
              AND tr.submitted_at &lt; #{toExclusive}
            <if test="categoryId != null">
              AND tr.category_id = #{categoryId}
            </if>
            GROUP BY tr.status
            ORDER BY tr.status
            </script>
            """)
    List<StatisticsStatusRow> selectStatusDistribution(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            @Param("categoryId") Long categoryId);

    @Select("""
            <script>
            SELECT c.id AS category_id,
                   c.name AS category_name,
                   COUNT(*) AS count
            FROM tech_request tr
            JOIN category c ON c.id = tr.category_id
            WHERE tr.submitted_at &gt;= #{fromInclusive}
              AND tr.submitted_at &lt; #{toExclusive}
            <if test="categoryId != null">
              AND tr.category_id = #{categoryId}
            </if>
            GROUP BY c.id, c.name, c.sort_order
            ORDER BY count DESC, c.sort_order ASC, c.id ASC
            </script>
            """)
    List<StatisticsCategoryRow> selectCategoryDistribution(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            @Param("categoryId") Long categoryId);

    @Select("""
            <script>
            SELECT DATE(DATE_ADD(tr.submitted_at, INTERVAL 8 HOUR)) AS statistic_date,
                   COUNT(*) AS count
            FROM tech_request tr
            WHERE tr.submitted_at &gt;= #{fromInclusive}
              AND tr.submitted_at &lt; #{toExclusive}
            <if test="categoryId != null">
              AND tr.category_id = #{categoryId}
            </if>
            GROUP BY statistic_date
            ORDER BY statistic_date ASC
            </script>
            """)
    List<StatisticsTrendRow> selectSubmissionTrend(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            @Param("categoryId") Long categoryId);
}
