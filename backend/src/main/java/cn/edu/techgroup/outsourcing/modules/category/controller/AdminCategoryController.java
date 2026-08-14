package cn.edu.techgroup.outsourcing.modules.category.controller;

import cn.edu.techgroup.outsourcing.common.api.ApiResponse;
import cn.edu.techgroup.outsourcing.modules.category.dto.CreateAdminCategoryCommand;
import cn.edu.techgroup.outsourcing.modules.category.dto.UpdateAdminCategoryCommand;
import cn.edu.techgroup.outsourcing.modules.category.dto.UpdateAdminCategoryStatusCommand;
import cn.edu.techgroup.outsourcing.modules.category.service.AdminCategoryService;
import cn.edu.techgroup.outsourcing.modules.category.vo.AdminCategoryVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final AdminCategoryService categoryService;

    public AdminCategoryController(AdminCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<AdminCategoryVO>> list(
            @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.success(categoryService.list(operator));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminCategoryVO> create(
            @Valid @RequestBody CreateAdminCategoryCommand command,
            @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.success(categoryService.create(command, operator));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminCategoryVO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAdminCategoryCommand command,
            @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.success(
                categoryService.update(id, command, operator));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<AdminCategoryVO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAdminCategoryStatusCommand command,
            @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.success(
                categoryService.updateStatus(id, command, operator));
    }
}
