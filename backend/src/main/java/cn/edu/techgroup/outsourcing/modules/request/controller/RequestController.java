package cn.edu.techgroup.outsourcing.modules.request.controller;

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

import cn.edu.techgroup.outsourcing.common.api.ApiResponse;
import cn.edu.techgroup.outsourcing.common.api.PageResponse;
import cn.edu.techgroup.outsourcing.modules.request.dto.CreateRequestCommand;
import cn.edu.techgroup.outsourcing.modules.request.dto.CancelRequestCommand;
import cn.edu.techgroup.outsourcing.modules.request.dto.RequestListQuery;
import cn.edu.techgroup.outsourcing.modules.request.dto.SaveDraftCommand;
import cn.edu.techgroup.outsourcing.modules.request.dto.SubmitRequestCommand;
import cn.edu.techgroup.outsourcing.modules.request.dto.UpdateRequestCommand;
import cn.edu.techgroup.outsourcing.modules.request.service.RequestService;
import cn.edu.techgroup.outsourcing.modules.request.service.RequesterRequestLifecycleService;
import cn.edu.techgroup.outsourcing.modules.request.vo.CreatedRequestVO;
import cn.edu.techgroup.outsourcing.modules.request.vo.RequestDetailVO;
import cn.edu.techgroup.outsourcing.modules.request.vo.RequestSummaryVO;
import cn.edu.techgroup.outsourcing.modules.request.vo.RequestMutationVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/requests")
public class RequestController {

    private final RequestService requestService;
    private final RequesterRequestLifecycleService lifecycleService;

    public RequestController(
            RequestService requestService,
            RequesterRequestLifecycleService lifecycleService) {
        this.requestService = requestService;
        this.lifecycleService = lifecycleService;
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

    @PostMapping("/drafts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN')")
    public ApiResponse<CreatedRequestVO> createDraft(
            @Valid @RequestBody SaveDraftCommand command,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(lifecycleService.createDraft(command, loginUser));
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

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN')")
    public ApiResponse<RequestMutationVO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRequestCommand command,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(lifecycleService.update(id, command, loginUser));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN')")
    public ApiResponse<RequestMutationVO> submit(
            @PathVariable Long id,
            @Valid @RequestBody SubmitRequestCommand command,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(lifecycleService.submit(id, command, loginUser));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN')")
    public ApiResponse<RequestMutationVO> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelRequestCommand command,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(lifecycleService.cancel(id, command, loginUser));
    }
}
