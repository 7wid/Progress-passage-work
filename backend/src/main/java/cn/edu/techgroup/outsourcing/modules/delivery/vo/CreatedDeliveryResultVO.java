package cn.edu.techgroup.outsourcing.modules.delivery.vo;

import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;

public record CreatedDeliveryResultVO(
        DeliveryVO delivery,
        RequestStatus requestStatus,
        int requestVersion) {
}
