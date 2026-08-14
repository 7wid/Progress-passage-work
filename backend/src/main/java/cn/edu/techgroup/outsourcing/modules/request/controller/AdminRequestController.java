package cn.edu.techgroup.outsourcing.modules.request.controller;

import cn.edu.techgroup.outsourcing.common.api.ApiResponse;
import cn.edu.techgroup.outsourcing.modules.request.dto.AdminRequestActionCommand;
import cn.edu.techgroup.outsourcing.modules.request.service.AdminRequestService;
import cn.edu.techgroup.outsourcing.modules.request.vo.AdminRequestActionVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/requests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRequestController {

    private final AdminRequestService requestService;

    public AdminRequestController(AdminRequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<AdminRequestActionVO> cancel(
            @PathVariable Long id,
            @Valid @RequestBody AdminRequestActionCommand command,
            @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.success(
                requestService.cancel(id, command, operator));
    }

    @PostMapping("/{id}/reopen")
    public ApiResponse<AdminRequestActionVO> reopen(
            @PathVariable Long id,
            @Valid @RequestBody AdminRequestActionCommand command,
            @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.success(
                requestService.reopen(id, command, operator));
    }
}
