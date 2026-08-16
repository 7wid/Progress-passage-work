package cn.edu.techgroup.outsourcing.modules.request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CancelRequestCommand(
        @NotNull @PositiveOrZero Integer expectedVersion,
        @NotBlank @Size(min = 5, max = 500) String reason) {

    public CancelRequestCommand {
        reason = reason == null ? null : reason.trim();
    }
}
