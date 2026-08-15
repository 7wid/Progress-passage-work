package cn.edu.techgroup.outsourcing.modules.statistics.vo;

import cn.edu.techgroup.outsourcing.modules.request.enums.RequestStatus;

public record StatusCountVO(RequestStatus status, long count) {}
