package cn.edu.techgroup.outsourcing.modules.request.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SubmitRequestCommand(
        @NotNull @PositiveOrZero Integer expectedVersion,
        @NotNull @AssertTrue(message = "提交前必须确认信息真实有效")
        Boolean informationConfirmed) {
}
