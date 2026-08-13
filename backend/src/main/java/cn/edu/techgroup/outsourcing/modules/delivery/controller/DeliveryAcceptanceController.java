package cn.edu.techgroup.outsourcing.modules.delivery.controller;

import cn.edu.techgroup.outsourcing.common.api.ApiResponse;
import cn.edu.techgroup.outsourcing.modules.delivery.dto.CreateAcceptanceCommand;
import cn.edu.techgroup.outsourcing.modules.delivery.dto.CreateDeliveryCommand;
import cn.edu.techgroup.outsourcing.modules.delivery.service.DeliveryAcceptanceService;
import cn.edu.techgroup.outsourcing.modules.delivery.vo.CreatedAcceptanceResultVO;
import cn.edu.techgroup.outsourcing.modules.delivery.vo.CreatedDeliveryResultVO;
import cn.edu.techgroup.outsourcing.modules.delivery.vo.DeliveryAcceptanceSnapshotVO;
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
@RequestMapping("/api/v1/requests/{requestId}")
public class DeliveryAcceptanceController {

    private final DeliveryAcceptanceService service;

    public DeliveryAcceptanceController(DeliveryAcceptanceService service) {
        this.service = service;
    }

    @GetMapping("/delivery-acceptance")
    @PreAuthorize("hasAnyRole('REQUESTER', 'MEMBER', 'ADMIN')")
    public ApiResponse<DeliveryAcceptanceSnapshotVO> get(
            @PathVariable Long requestId,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(service.get(requestId, loginUser));
    }

    @PostMapping("/deliveries")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    public ApiResponse<CreatedDeliveryResultVO> createDelivery(
            @PathVariable Long requestId,
            @Valid @RequestBody CreateDeliveryCommand command,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(
                service.createDelivery(requestId, command, loginUser));
    }

    @PostMapping("/acceptance")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN')")
    public ApiResponse<CreatedAcceptanceResultVO> createAcceptance(
            @PathVariable Long requestId,
            @Valid @RequestBody CreateAcceptanceCommand command,
            @AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.success(
                service.createAcceptance(requestId, command, loginUser));
    }
}
