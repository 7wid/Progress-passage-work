package cn.edu.techgroup.outsourcing.modules.delivery.dto;

import cn.edu.techgroup.outsourcing.modules.delivery.enums.AcceptanceResult;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateAcceptanceCommand(
        @NotNull
        @PositiveOrZero
        Integer requestVersion,

        @NotNull
        AcceptanceResult result,

        @Size(max = 2000)
        String comment) {

    public CreateAcceptanceCommand {
        comment = trimToNull(comment);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
