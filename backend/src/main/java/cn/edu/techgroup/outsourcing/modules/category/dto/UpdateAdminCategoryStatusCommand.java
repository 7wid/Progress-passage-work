package cn.edu.techgroup.outsourcing.modules.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateAdminCategoryStatusCommand(
        @NotNull Instant expectedUpdatedAt,
        @NotNull Boolean enabled,
        @NotBlank @Size(min = 5, max = 500) String reason) {}
