package cn.edu.techgroup.outsourcing.modules.assignment.controller;

import cn.edu.techgroup.outsourcing.common.api.ApiResponse;
import cn.edu.techgroup.outsourcing.modules.assignment.service.AssignmentService;
import cn.edu.techgroup.outsourcing.modules.assignment.vo.MemberOptionVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/members")
public class MemberOptionController {

    private final AssignmentService assignmentService;

    public MemberOptionController(
            AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping("/options")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<MemberOptionVO>> options(
            @RequestParam(defaultValue = "")
            @Size(max = 64)
            String keyword,
            @AuthenticationPrincipal LoginUser loginUser) {

        return ApiResponse.success(
                assignmentService.listMemberOptions(
                        keyword,
                        loginUser));
    }
}