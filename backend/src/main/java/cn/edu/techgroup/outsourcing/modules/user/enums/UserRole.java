package cn.edu.techgroup.outsourcing.modules.user.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;

public enum UserRole implements IEnum<String> {
    REQUESTER("REQUESTER"),
    MEMBER("MEMBER"),
    ADMIN("ADMIN");

    @EnumValue
    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
