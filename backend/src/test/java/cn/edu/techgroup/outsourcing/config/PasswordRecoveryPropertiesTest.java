package cn.edu.techgroup.outsourcing.config;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.Validation;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class PasswordRecoveryPropertiesTest {
    @Test
    void disabledFeatureAllowsEmptyMailConfiguration() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(new PasswordRecoveryProperties(
                    false, "", "", Duration.ofMinutes(15))).isEmpty());
        }
    }

    @Test
    void enabledFeatureRequiresTrustedUrlAndBoundedLifetime() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(new PasswordRecoveryProperties(true, "security@example.org",
                    "https://example.org/reset-password", Duration.ofMinutes(15))).isEmpty());
            for (String url : new String[]{"http://example.org/reset-password", "https://example.org/reset-password?next=x",
                    "https://user:pass@example.org/reset-password", "https://example.org/reset-password#x", ""}) {
                assertFalse(validator.validate(new PasswordRecoveryProperties(true, "security@example.org", url,
                        Duration.ofMinutes(15))).isEmpty());
            }
            assertFalse(validator.validate(new PasswordRecoveryProperties(true, "security@example.org",
                    "https://example.org/reset-password", Duration.ofHours(1))).isEmpty());
        }
    }
}
