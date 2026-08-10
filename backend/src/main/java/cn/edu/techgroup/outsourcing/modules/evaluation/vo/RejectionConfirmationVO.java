package cn.edu.techgroup.outsourcing.modules.evaluation.vo;

import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;

public record RejectionConfirmationVO(
        String requestId,
        RequestStatus requestStatus,
        int requestVersion) {
}