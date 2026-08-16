package cn.edu.techgroup.outsourcing.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUserCommand(
        @NotBlank @Size(max = 64) @Pattern(regexp = "^[A-Za-z0-9._-]+$") String account,
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$") String password,
        @NotBlank @Size(max = 80) String displayName,
        @NotBlank @Email @Size(max = 160) String email,
        @Size(max = 32) String phone,
        @Size(max = 160) String department) {

    public RegisterUserCommand {
        account = account == null ? null : account.trim();
        displayName = displayName == null ? null : displayName.trim();
        email = email == null ? null : email.trim();
        phone = trimToNull(phone);
        department = trimToNull(department);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
