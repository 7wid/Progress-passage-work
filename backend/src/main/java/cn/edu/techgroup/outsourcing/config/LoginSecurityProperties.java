package cn.edu.techgroup.outsourcing.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.login-security")
public record LoginSecurityProperties(
        @Min(3) @Max(20) int maxFailedAttempts,
        @NotNull Duration lockDuration) {

    public LoginSecurityProperties {
        if (lockDuration != null && (lockDuration.isZero() || lockDuration.isNegative())) {
            throw new IllegalArgumentException("app.login-security.lock-duration must be positive");
        }
    }
}
