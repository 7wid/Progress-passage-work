package cn.edu.techgroup.outsourcing.modules.delivery.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;

public enum AcceptanceResult implements IEnum<String> {
    ACCEPTED("ACCEPTED"),
    REWORK_REQUIRED("REWORK_REQUIRED");

    @EnumValue
    private final String value;

    AcceptanceResult(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
