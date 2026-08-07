package cn.edu.techgroup.outsourcing.modules.request.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;

public enum RequestStatus implements IEnum<String> {
    DRAFT("DRAFT"),
    PENDING_REVIEW("PENDING_REVIEW"),
    NEED_MORE_INFO("NEED_MORE_INFO"),
    PENDING_ASSIGNMENT("PENDING_ASSIGNMENT"),
    IN_PROGRESS("IN_PROGRESS"),
    PENDING_ACCEPTANCE("PENDING_ACCEPTANCE"),
    COMPLETED("COMPLETED"),
    REJECTED("REJECTED"),
    CANCELLED("CANCELLED");

    @EnumValue
    private final String value;

    RequestStatus(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
