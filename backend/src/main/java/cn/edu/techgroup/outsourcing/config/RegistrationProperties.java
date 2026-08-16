package cn.edu.techgroup.outsourcing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.registration")
public record RegistrationProperties(boolean enabled, String emailSuffix) {
}
