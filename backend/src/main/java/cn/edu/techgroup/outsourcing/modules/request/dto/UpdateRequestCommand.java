package cn.edu.techgroup.outsourcing.modules.request.dto;

import cn.edu.techgroup.outsourcing.modules.request.enums.RequestUrgency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateRequestCommand(
        @NotNull @PositiveOrZero Integer expectedVersion,
        @Positive Long categoryId,
        @Size(max = 80) String title,
        @Size(max = 1000) String background,
        @Size(max = 5000) String description,
        @Size(max = 3000) String expectedResult,
        LocalDate expectedDeadline,
        RequestUrgency urgency,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal budgetAmount,
        @Size(max = 120) String budgetDescription,
        @Size(max = 5000) String technicalConstraints,
        @Size(max = 255) String contactInfo) {

    public UpdateRequestCommand {
        title = trimToNull(title);
        background = trimToNull(background);
        description = trimToNull(description);
        expectedResult = trimToNull(expectedResult);
        budgetDescription = trimToNull(budgetDescription);
        technicalConstraints = trimToNull(technicalConstraints);
        contactInfo = trimToNull(contactInfo);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
