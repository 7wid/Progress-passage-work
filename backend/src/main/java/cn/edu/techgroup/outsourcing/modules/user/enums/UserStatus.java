package cn.edu.techgroup.outsourcing.modules.user.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;

public enum UserStatus implements IEnum<String> {
    ACTIVE("ACTIVE"),
    DISABLED("DISABLED");

    @EnumValue
    private final String value;

    UserStatus(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
