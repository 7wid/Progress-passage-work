package cn.edu.techgroup.outsourcing.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileCommand(
        @NotBlank @Size(max = 80) String displayName,
        @Email @Size(max = 160) String email,
        @Size(max = 32) String phone,
        @Size(max = 160) String department) {

    public UpdateMyProfileCommand {
        displayName = displayName == null ? null : displayName.trim();
        email = trimToNull(email);
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
