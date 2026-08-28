package cn.edu.techgroup.outsourcing.modules.user.controller;

import cn.edu.techgroup.outsourcing.common.api.ApiResponse;
import cn.edu.techgroup.outsourcing.common.api.PageResponse;
import cn.edu.techgroup.outsourcing.modules.user.dto.AdminRequesterListQuery;
import cn.edu.techgroup.outsourcing.modules.user.dto.CreateRequesterCommand;
import cn.edu.techgroup.outsourcing.modules.user.dto.UpdateRequesterStatusCommand;
import cn.edu.techgroup.outsourcing.modules.user.service.AdminRequesterService;
import cn.edu.techgroup.outsourcing.modules.user.vo.AdminRequesterVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/requesters")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRequesterController {
    private final AdminRequesterService service;

    public AdminRequesterController(AdminRequesterService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminRequesterVO>> list(
            @Valid @ModelAttribute AdminRequesterListQuery query,
            @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.success(service.list(query, operator));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminRequesterVO> create(
            @Valid @RequestBody CreateRequesterCommand command,
            @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.success(service.create(command, operator));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<AdminRequesterVO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRequesterStatusCommand command,
            @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.success(service.updateStatus(id, command, operator));
    }
}
