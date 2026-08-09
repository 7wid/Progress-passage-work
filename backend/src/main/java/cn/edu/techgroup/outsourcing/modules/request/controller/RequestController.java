package cn.edu.techgroup.outsourcing.modules.request.controller;

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

import cn.edu.techgroup.outsourcing.common.api.ApiResponse;
import cn.edu.techgroup.outsourcing.common.api.PageResponse;
import cn.edu.techgroup.outsourcing.modules.request.dto.CreateRequestCommand;
import cn.edu.techgroup.outsourcing.modules.request.dto.RequestListQuery;
import cn.edu.techgroup.outsourcing.modules.request.service.RequestService;
import cn.edu.techgroup.outsourcing.modules.request.vo.CreatedRequestVO;
import cn.edu.techgroup.outsourcing.modules.request.vo.RequestDetailVO;
import cn.edu.techgroup.outsourcing.modules.request.vo.RequestSummaryVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/requests")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN')")
    public ApiResponse<CreatedRequestVO> create(
            @Valid @RequestBody CreateRequestCommand command,
            @AuthenticationPrincipal LoginUser loginUser) {

        return ApiResponse.success(
                requestService.createAndSubmit(command, loginUser));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('REQUESTER', 'MEMBER', 'ADMIN')")
    public ApiResponse<PageResponse<RequestSummaryVO>> list(
            @Valid @ModelAttribute RequestListQuery query,
            @AuthenticationPrincipal LoginUser loginUser) {

        return ApiResponse.success(
                requestService.list(query, loginUser));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('REQUESTER', 'MEMBER', 'ADMIN')")
    public ApiResponse<RequestDetailVO> detail(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser loginUser) {

        return ApiResponse.success(
                requestService.getDetail(id, loginUser));
    }
}
