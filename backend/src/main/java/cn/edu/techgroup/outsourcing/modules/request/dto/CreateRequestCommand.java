package cn.edu.techgroup.outsourcing.modules.request.dto;

import cn.edu.techgroup.outsourcing.modules.request.enums.RequestUrgency;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateRequestCommand(

        @NotNull
        @Positive
        Long categoryId,

        @NotBlank
        @Size(min = 5, max = 80)
        String title,

        @NotBlank
        @Size(min = 20, max = 1000)
        String background,

        @NotBlank
        @Size(min = 50, max = 5000)
        String description,

        @NotBlank
        @Size(min = 5, max = 3000)
        String expectedResult,

        @NotNull
        @FutureOrPresent
        LocalDate expectedDeadline,

        @NotNull
        RequestUrgency urgency,

        @DecimalMin("0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal budgetAmount,

        @Size(max = 120)
        String budgetDescription,

        @Size(max = 5000)
        String technicalConstraints,

        @NotBlank
        @Size(max = 255)
        String contactInfo,

        @NotNull
        @AssertTrue(message = "提交前必须确认信息真实有效")
        Boolean informationConfirmed) {
}