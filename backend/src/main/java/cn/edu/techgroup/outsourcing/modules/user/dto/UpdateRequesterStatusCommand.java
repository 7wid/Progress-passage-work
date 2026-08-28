package cn.edu.techgroup.outsourcing.modules.user.dto;

import cn.edu.techgroup.outsourcing.modules.user.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateRequesterStatusCommand(
        @NotNull Instant expectedUpdatedAt,
        @NotNull UserStatus status,
        @NotBlank @Size(min = 5, max = 500) String reason) {

    public UpdateRequesterStatusCommand {
        reason = reason == null ? null : reason.trim();
    }
}
