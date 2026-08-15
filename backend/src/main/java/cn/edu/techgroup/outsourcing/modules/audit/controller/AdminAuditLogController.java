package cn.edu.techgroup.outsourcing.modules.audit.controller;

import cn.edu.techgroup.outsourcing.common.api.ApiResponse;
import cn.edu.techgroup.outsourcing.common.api.PageResponse;
import cn.edu.techgroup.outsourcing.modules.audit.dto.AdminAuditLogQuery;
import cn.edu.techgroup.outsourcing.modules.audit.service.AdminAuditLogService;
import cn.edu.techgroup.outsourcing.modules.audit.vo.AuditLogVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditLogController {

    private final AdminAuditLogService auditLogService;

    public AdminAuditLogController(AdminAuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AuditLogVO>> list(
            @Valid @ModelAttribute AdminAuditLogQuery query,
            @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.success(auditLogService.list(query, operator));
    }
}
