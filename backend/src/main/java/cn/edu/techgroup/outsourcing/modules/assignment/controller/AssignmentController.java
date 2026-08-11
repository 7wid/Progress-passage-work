package cn.edu.techgroup.outsourcing.modules.assignment.controller;

import cn.edu.techgroup.outsourcing.common.api.ApiResponse;
import cn.edu.techgroup.outsourcing.modules.assignment.dto.UpdateRequestMembersCommand;
import cn.edu.techgroup.outsourcing.modules.assignment.service.AssignmentService;
import cn.edu.techgroup.outsourcing.modules.assignment.vo.RequestAssignmentVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/requests/{requestId}/members")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(
            AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    @PreAuthorize(
            "hasAnyRole('REQUESTER', 'MEMBER', 'ADMIN')")
    public ApiResponse<RequestAssignmentVO> get(
            @PathVariable Long requestId,
            @AuthenticationPrincipal LoginUser loginUser) {

        return ApiResponse.success(
                assignmentService.get(
                        requestId,
                        loginUser));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RequestAssignmentVO> update(
            @PathVariable Long requestId,
            @Valid
            @RequestBody
            UpdateRequestMembersCommand command,
            @AuthenticationPrincipal LoginUser loginUser) {

        return ApiResponse.success(
                assignmentService.update(
                        requestId,
                        command,
                        loginUser));
    }
}