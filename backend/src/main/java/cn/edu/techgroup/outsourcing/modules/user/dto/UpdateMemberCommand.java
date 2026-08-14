package cn.edu.techgroup.outsourcing.modules.user.dto;

import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record UpdateMemberCommand(
        @NotNull Instant expectedUpdatedAt,
        @NotBlank @Size(max = 80) String displayName,
        @Email @Size(max = 160) String email,
        @Size(max = 32) String phone,
        @Size(max = 160) String department,
        @NotNull UserRole role,
        @NotNull @Size(max = 20) List<@Positive Long> skillIds,
        @NotBlank @Size(min = 5, max = 500) String reason) {}
