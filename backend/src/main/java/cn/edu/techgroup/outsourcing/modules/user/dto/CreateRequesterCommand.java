package cn.edu.techgroup.outsourcing.modules.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

/** The role is intentionally absent: controlled onboarding only creates REQUESTER accounts. */
public record CreateRequesterCommand(
        @NotBlank @Size(min = 3, max = 64)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$") String account,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$") String initialPassword,
        @NotBlank @Size(min = 2, max = 80) String displayName,
        @Email @Size(max = 160) String email,
        @Pattern(regexp = "^[0-9+()\\-\\s]{6,32}$") String phone,
        @Size(max = 160) String department,
        @NotBlank @Size(min = 5, max = 500) String reason) {

    public CreateRequesterCommand {
        account = normalize(account);
        if (account != null) account = account.toLowerCase(Locale.ROOT);
        displayName = normalize(displayName);
        email = normalize(email);
        if (email != null) email = email.toLowerCase(Locale.ROOT);
        phone = normalize(phone);
        department = normalize(department);
        reason = normalize(reason);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Override
    public String toString() {
        return "CreateRequesterCommand[initialPassword=[REDACTED]]";
    }
}
