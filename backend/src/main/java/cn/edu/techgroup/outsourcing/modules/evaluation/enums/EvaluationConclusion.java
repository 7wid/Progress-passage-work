package cn.edu.techgroup.outsourcing.modules.evaluation.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;

public enum EvaluationConclusion implements IEnum<String> {

    FEASIBLE("FEASIBLE"),
    NEED_MORE_INFO("NEED_MORE_INFO"),
    NOT_FEASIBLE("NOT_FEASIBLE");

    @EnumValue
    private final String value;

    EvaluationConclusion(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}