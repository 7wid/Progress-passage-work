package cn.edu.techgroup.outsourcing.modules.assignment.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;

public enum RequestMemberType implements IEnum<String> {

    OWNER("OWNER"),
    PARTICIPANT("PARTICIPANT");

    @EnumValue
    private final String value;

    RequestMemberType(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}