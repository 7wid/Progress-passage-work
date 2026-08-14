package cn.edu.techgroup.outsourcing.modules.request.vo;

import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;

public record AdminRequestActionVO(
        String id,
        RequestStatus status,
        int version) {}
