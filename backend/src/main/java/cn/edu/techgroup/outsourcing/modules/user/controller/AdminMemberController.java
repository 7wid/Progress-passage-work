package cn.edu.techgroup.outsourcing.modules.user.controller;

import cn.edu.techgroup.outsourcing.common.api.ApiResponse;
import cn.edu.techgroup.outsourcing.common.api.PageResponse;
import cn.edu.techgroup.outsourcing.modules.user.dto.AdminMemberListQuery;
import cn.edu.techgroup.outsourcing.modules.user.dto.CreateMemberCommand;
import cn.edu.techgroup.outsourcing.modules.user.dto.UpdateMemberCommand;
import cn.edu.techgroup.outsourcing.modules.user.dto.UpdateMemberStatusCommand;
import cn.edu.techgroup.outsourcing.modules.user.service.AdminMemberService;
import cn.edu.techgroup.outsourcing.modules.user.vo.AdminMemberVO;
import cn.edu.techgroup.outsourcing.modules.user.vo.SkillTagVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMemberController {

    private final AdminMemberService memberService;

    public AdminMemberController(AdminMemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/members")
    public ApiResponse<PageResponse<AdminMemberVO>> list(
            @Valid @ModelAttribute AdminMemberListQuery query,
            @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.success(memberService.list(query, operator));
    }

    @GetMapping("/members/{id}")
    public ApiResponse<AdminMemberVO> get(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.success(memberService.get(id, operator));
    }

    @PostMapping("/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminMemberVO> create(
            @Valid @RequestBody CreateMemberCommand command,
            @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.success(memberService.create(command, operator));
    }

    @PutMapping("/members/{id}")
    public ApiResponse<AdminMemberVO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMemberCommand command,
            @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.success(
                memberService.update(id, command, operator));
    }

    @PostMapping("/members/{id}/status")
    public ApiResponse<AdminMemberVO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMemberStatusCommand command,
            @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.success(
                memberService.updateStatus(id, command, operator));
    }

    @GetMapping("/skill-tags")
    public ApiResponse<List<SkillTagVO>> skillTags(
            @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.success(memberService.skillTags(operator));
    }
}
