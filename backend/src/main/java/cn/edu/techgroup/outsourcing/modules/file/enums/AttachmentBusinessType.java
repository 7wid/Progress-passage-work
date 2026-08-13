package cn.edu.techgroup.outsourcing.modules.file.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;

public enum AttachmentBusinessType implements IEnum<String> {
    REQUEST("REQUEST"),
    DELIVERY("DELIVERY");

    @EnumValue
    private final String value;

    AttachmentBusinessType(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
