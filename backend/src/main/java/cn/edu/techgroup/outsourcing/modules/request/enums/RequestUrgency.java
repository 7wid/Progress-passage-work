package cn.edu.techgroup.outsourcing.modules.request.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;

public enum RequestUrgency implements IEnum<String> {

    NORMAL("NORMAL"),
    HIGH("HIGH"),
    URGENT("URGENT");

    @EnumValue
    private final String value;

    RequestUrgency(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}