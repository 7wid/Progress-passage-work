package cn.edu.techgroup.outsourcing.modules.statistics.controller;

import cn.edu.techgroup.outsourcing.common.api.ApiResponse;
import cn.edu.techgroup.outsourcing.modules.statistics.dto.AdminStatisticsQuery;
import cn.edu.techgroup.outsourcing.modules.statistics.service.AdminStatisticsService;
import cn.edu.techgroup.outsourcing.modules.statistics.vo.AdminStatisticsDashboardVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/statistics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatisticsController {

    private final AdminStatisticsService statisticsService;

    public AdminStatisticsController(AdminStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping
    public ApiResponse<AdminStatisticsDashboardVO> dashboard(
            @Valid @ModelAttribute AdminStatisticsQuery query,
            @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.success(statisticsService.getDashboard(query, operator));
    }
}
