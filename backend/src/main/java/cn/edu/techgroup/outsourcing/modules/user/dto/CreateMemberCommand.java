package cn.edu.techgroup.outsourcing.modules.user.dto;

import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateMemberCommand(
        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$")
        String account,
        @NotBlank
        @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$")
        String initialPassword,
        @NotBlank @Size(max = 80) String displayName,
        @Email @Size(max = 160) String email,
        @Size(max = 32) String phone,
        @Size(max = 160) String department,
        @NotNull UserRole role,
        @NotNull @Size(max = 20) List<@Positive Long> skillIds,
        @NotBlank @Size(min = 5, max = 500) String reason) {}
