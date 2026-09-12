package cn.edu.techgroup.outsourcing.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.password-recovery")
public record PasswordRecoveryProperties(
        boolean enabled,
        String from,
        String resetUrl,
        @NotNull Duration tokenTtl) {

    @AssertTrue(message = "重置链接有效期须为 5～30 分钟")
    public boolean isTokenTtlValid() {
        return tokenTtl != null && tokenTtl.compareTo(Duration.ofMinutes(5)) >= 0
                && tokenTtl.compareTo(Duration.ofMinutes(30)) <= 0;
    }

    @AssertTrue(message = "开启密码找回时必须配置发件邮箱和可信 HTTPS 重置页面地址")
    public boolean isDeliveryConfigured() {
        if (!enabled) return true;
        if (from == null || !from.matches("[^\\s<>@]+@[^\\s<>@]+\\.[^\\s<>@]+")) return false;
        try {
            URI uri = URI.create(resetUrl);
            boolean secure = "https".equals(uri.getScheme());
            boolean local = "http".equals(uri.getScheme())
                    && ("localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost()));
            return (secure || local) && uri.getHost() != null && uri.getUserInfo() == null
                    && uri.getQuery() == null && uri.getFragment() == null
                    && "/reset-password".equals(uri.getPath());
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
