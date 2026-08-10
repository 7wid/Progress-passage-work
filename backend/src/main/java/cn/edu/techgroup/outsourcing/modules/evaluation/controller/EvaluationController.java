package cn.edu.techgroup.outsourcing.modules.evaluation.controller;

import cn.edu.techgroup.outsourcing.common.api.ApiResponse;
import cn.edu.techgroup.outsourcing.modules.evaluation.dto.ConfirmRejectionCommand;
import cn.edu.techgroup.outsourcing.modules.evaluation.dto.CreateEvaluationCommand;
import cn.edu.techgroup.outsourcing.modules.evaluation.service.EvaluationService;
import cn.edu.techgroup.outsourcing.modules.evaluation.vo.EvaluationResultVO;
import cn.edu.techgroup.outsourcing.modules.evaluation.vo.EvaluationVO;
import cn.edu.techgroup.outsourcing.modules.evaluation.vo.RejectionConfirmationVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/requests/{requestId}/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('REQUESTER', 'MEMBER', 'ADMIN')")
    public ApiResponse<List<EvaluationVO>> list(
            @PathVariable Long requestId,
            @AuthenticationPrincipal LoginUser loginUser) {

        return ApiResponse.success(
                evaluationService.list(requestId, loginUser));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    public ApiResponse<EvaluationResultVO> create(
            @PathVariable Long requestId,
            @Valid @RequestBody CreateEvaluationCommand command,
            @AuthenticationPrincipal LoginUser loginUser) {

        return ApiResponse.success(
                evaluationService.create(
                        requestId,
                        command,
                        loginUser));
    }

    @PostMapping("/{evaluationId}/confirm-rejection")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RejectionConfirmationVO> confirmRejection(
            @PathVariable Long requestId,
            @PathVariable Long evaluationId,
            @Valid @RequestBody ConfirmRejectionCommand command,
            @AuthenticationPrincipal LoginUser loginUser) {

        return ApiResponse.success(
                evaluationService.confirmRejection(
                        requestId,
                        evaluationId,
                        command,
                        loginUser));
    }
}