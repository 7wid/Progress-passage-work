package cn.edu.techgroup.outsourcing.modules.evaluation.vo;

import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;

public record EvaluationResultVO(
        EvaluationVO evaluation,
        RequestStatus requestStatus,
        int requestVersion,
        boolean adminConfirmationRequired) {
}