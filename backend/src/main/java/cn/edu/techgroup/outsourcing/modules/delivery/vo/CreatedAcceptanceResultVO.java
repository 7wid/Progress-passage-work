package cn.edu.techgroup.outsourcing.modules.delivery.vo;

import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;

public record CreatedAcceptanceResultVO(
        AcceptanceVO acceptance,
        RequestStatus requestStatus,
        int requestVersion) {
}
