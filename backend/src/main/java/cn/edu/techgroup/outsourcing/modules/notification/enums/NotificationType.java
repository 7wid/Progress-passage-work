package cn.edu.techgroup.outsourcing.modules.notification.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;

public enum NotificationType implements IEnum<String> {
    REQUEST_SUBMITTED("REQUEST_SUBMITTED"),
    INFO_REQUIRED("INFO_REQUIRED"),
    EVALUATION_COMPLETED("EVALUATION_COMPLETED"),
    REJECTION_CONFIRMATION_REQUIRED("REJECTION_CONFIRMATION_REQUIRED"),
    ASSIGNMENT_UPDATED("ASSIGNMENT_UPDATED"),
    PROGRESS_UPDATED("PROGRESS_UPDATED"),
    DELIVERY_SUBMITTED("DELIVERY_SUBMITTED"),
    ACCEPTANCE_COMPLETED("ACCEPTANCE_COMPLETED"),
    ADMIN_REQUEST_UPDATED("ADMIN_REQUEST_UPDATED");

    @EnumValue
    private final String value;

    NotificationType(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
