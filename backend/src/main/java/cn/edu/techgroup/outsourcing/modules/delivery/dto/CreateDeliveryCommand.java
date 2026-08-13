package cn.edu.techgroup.outsourcing.modules.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateDeliveryCommand(
        @NotNull
        @PositiveOrZero
        Integer requestVersion,

        @NotBlank
        @Size(min = 5, max = 5000)
        String description,

        @Size(max = 1000)
        String deliveryUrl) {

    public CreateDeliveryCommand {
        description = trim(description);
        deliveryUrl = trimToNull(deliveryUrl);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
