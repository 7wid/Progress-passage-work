package cn.edu.techgroup.outsourcing.modules.request.vo;

import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;

public record RequestMutationVO(
        String id,
        String requestNo,
        RequestStatus status,
        int version) {
}
