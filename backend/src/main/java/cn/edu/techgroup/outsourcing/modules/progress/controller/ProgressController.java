package cn.edu.techgroup.outsourcing.modules.progress.controller;

import cn.edu.techgroup.outsourcing.common.api.ApiResponse;
import cn.edu.techgroup.outsourcing.modules.progress.dto.CreateProgressCommand;
import cn.edu.techgroup.outsourcing.modules.progress.service.ProgressService;
import cn.edu.techgroup.outsourcing.modules.progress.vo.CreatedProgressResultVO;
import cn.edu.techgroup.outsourcing.modules.progress.vo.RequestProgressSnapshotVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/requests/{requestId}/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('REQUESTER', 'MEMBER', 'ADMIN')")
    public ApiResponse<RequestProgressSnapshotVO> get(
            @PathVariable Long requestId,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(progressService.get(requestId, loginUser));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    public ApiResponse<CreatedProgressResultVO> create(
            @PathVariable Long requestId,
            @Valid @RequestBody CreateProgressCommand command,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(
                progressService.create(requestId, command, loginUser));
    }
}
