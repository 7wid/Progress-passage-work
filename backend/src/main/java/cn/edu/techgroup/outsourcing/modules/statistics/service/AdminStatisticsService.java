package cn.edu.techgroup.outsourcing.modules.statistics.service;

import cn.edu.techgroup.outsourcing.modules.statistics.dto.AdminStatisticsQuery;
import cn.edu.techgroup.outsourcing.modules.statistics.vo.AdminStatisticsDashboardVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;

public interface AdminStatisticsService {

    AdminStatisticsDashboardVO getDashboard(
            AdminStatisticsQuery query,
            LoginUser operator);
}
