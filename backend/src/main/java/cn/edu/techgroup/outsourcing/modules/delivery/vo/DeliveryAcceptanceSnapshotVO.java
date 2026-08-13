package cn.edu.techgroup.outsourcing.modules.delivery.vo;

import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;
import java.util.List;

public record DeliveryAcceptanceSnapshotVO(
        String requestId,
        RequestStatus requestStatus,
        int requestVersion,
        boolean canSubmitDelivery,
        boolean canAccept,
        List<DeliveryVO> deliveries,
        List<AcceptanceVO> acceptances) {
}
