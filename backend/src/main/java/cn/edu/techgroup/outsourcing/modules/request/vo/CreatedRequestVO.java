package cn.edu.techgroup.outsourcing.modules.request.vo;

import cn.edu.techgroup.outsourcing.modules.request.entity.RequestEntity;
import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;

public record CreatedRequestVO(
        String id,
        String requestNo,
        RequestStatus status) {

    public static CreatedRequestVO from(RequestEntity entity) {
        return new CreatedRequestVO(
                entity.getId().toString(),
                entity.getRequestNo(),
                entity.getStatus());
    }
}