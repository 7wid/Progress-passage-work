package cn.edu.techgroup.outsourcing.modules.audit.service;

import cn.edu.techgroup.outsourcing.common.api.PageResponse;
import cn.edu.techgroup.outsourcing.modules.audit.dto.AdminAuditLogQuery;
import cn.edu.techgroup.outsourcing.modules.audit.vo.AuditLogVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;

public interface AdminAuditLogService {

    PageResponse<AuditLogVO> list(AdminAuditLogQuery query, LoginUser operator);
}
