package cn.edu.techgroup.outsourcing.modules.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.List;

public record CreateDeliveryCommand(
        @NotNull
        @PositiveOrZero
        Integer requestVersion,

        @NotBlank
        @Size(min = 5, max = 5000)
        String description,

        @Size(max = 1000)
        String deliveryUrl,

        @Size(max = 5)
        List<Long> attachmentIds) {

    public CreateDeliveryCommand {
        description = trim(description);
        deliveryUrl = trimToNull(deliveryUrl);
        attachmentIds = attachmentIds == null
                ? List.of()
                : List.copyOf(new LinkedHashSet<>(attachmentIds));
    }

    public CreateDeliveryCommand(
            Integer requestVersion,
            String description,
            String deliveryUrl) {
        this(requestVersion, description, deliveryUrl, List.of());
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
