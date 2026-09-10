package cn.edu.techgroup.outsourcing.modules.auth.service;

import cn.edu.techgroup.outsourcing.config.PasswordRecoveryProperties;
import cn.edu.techgroup.outsourcing.modules.auth.mapper.PasswordRecoveryMapper;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserStatus;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RecoveryMailDelivery {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecoveryMailDelivery.class);
    private final PasswordRecoveryProperties properties;
    private final UserMapper userMapper;
    private final PasswordRecoveryMapper mapper;
    private final ObjectProvider<JavaMailSender> senderProvider;

    public RecoveryMailDelivery(PasswordRecoveryProperties properties, UserMapper userMapper,
            PasswordRecoveryMapper mapper, ObjectProvider<JavaMailSender> senderProvider) {
        this.properties = properties;
        this.userMapper = userMapper;
        this.mapper = mapper;
        this.senderProvider = senderProvider;
    }

    @PostConstruct
    public void validateConfiguration() {
        if (!properties.enabled()) return;
        JavaMailSender sender = senderProvider.getIfAvailable();
        if (sender == null || (sender instanceof JavaMailSenderImpl smtp
                && !StringUtils.hasText(smtp.getHost()))) {
            throw new IllegalStateException("开启密码找回必须配置 SMTP_HOST");
        }
        if (sender instanceof JavaMailSenderImpl smtp
                && !"localhost".equals(smtp.getHost()) && !"127.0.0.1".equals(smtp.getHost())) {
            var mailProperties = smtp.getJavaMailProperties();
            boolean ssl = Boolean.parseBoolean(mailProperties.getProperty("mail.smtp.ssl.enable"));
            boolean startTls = Boolean.parseBoolean(mailProperties.getProperty("mail.smtp.starttls.enable"))
                    && Boolean.parseBoolean(mailProperties.getProperty("mail.smtp.starttls.required"));
            if (!ssl && !startTls) {
                throw new IllegalStateException("非本机 SMTP 必须使用 TLS 加密连接");
            }
        }
    }

    // Runs in a bounded executor. No transaction spans the SMTP network call.
    public void deliver(String email) {
        String tokenHash = null;
        try {
            UserEntity user = userMapper.selectByRecoveryEmail(email);
            if (user == null || user.getStatus() != UserStatus.ACTIVE) return;
            String token = RecoveryTokens.generate();
            tokenHash = RecoveryTokens.hash(token);
            mapper.save(new PasswordRecoveryMapper.Token(user.getId(), tokenHash, user.getEmail(),
                    RecoveryTokens.hash(user.getPasswordHash()), Instant.now().plus(properties.tokenTtl())));
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.from());
            message.setTo(user.getEmail());
            message.setSubject("需求服务平台 · 重置密码");
            message.setText("您好：\n\n我们收到了您的密码重置申请。请打开下方链接设置新密码：\n\n"
                    + properties.resetUrl() + "#token=" + token
                    + "\n\n链接将在 " + properties.tokenTtl().toMinutes()
                    + " 分钟后失效，且只能使用一次。重新申请后请使用最新邮件中的链接。"
                    + "\n如果您没有申请重置，请忽略本邮件，您的密码不会因此改变。"
                    + "\n请勿转发此邮件或向任何人提供链接。\n");
            senderProvider.getObject().send(message);
        } catch (RuntimeException exception) {
            // SMTP exceptions can contain recipients or message content; never log their payload.
            LOGGER.error("Password recovery mail delivery failed: type={}", exception.getClass().getSimpleName());
            if (tokenHash != null) {
                try {
                    mapper.deleteToken(tokenHash);
                } catch (RuntimeException cleanupFailure) {
                    LOGGER.error("Password recovery token cleanup failed: type={}",
                            cleanupFailure.getClass().getSimpleName());
                }
            }
        }
    }

    public void notifyReset(String email) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.from());
            message.setTo(email);
            message.setSubject("需求服务平台 · 密码已重置");
            message.setText("您的账号密码已通过邮箱验证重置，旧登录状态已失效。\n"
                    + "如果不是您本人操作，请立即联系平台管理员核实账号安全，并检查邮箱安全设置。\n"
                    + "平台不会通过邮件索取您的密码。\n");
            senderProvider.getObject().send(message);
        } catch (RuntimeException exception) {
            LOGGER.error("Password reset notification failed: type={}", exception.getClass().getSimpleName());
        }
    }
}
