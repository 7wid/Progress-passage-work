package cn.edu.techgroup.outsourcing.modules.auth.service;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.config.PasswordRecoveryProperties;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditActions;
import cn.edu.techgroup.outsourcing.modules.auth.dto.ResetPasswordCommand;
import cn.edu.techgroup.outsourcing.modules.auth.mapper.PasswordRecoveryMapper;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserStatus;
import cn.edu.techgroup.outsourcing.modules.user.event.PasswordChangedEvent;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordRecoveryService {
    private final PasswordRecoveryProperties properties;
    private final RecoveryRateLimiter limiter;
    private final TaskExecutor executor;
    private final RecoveryMailDelivery delivery;
    private final PasswordRecoveryMapper mapper;
    private final UserMapper userMapper;
    private final PasswordEncoder encoder;
    private final AuditRecorder auditRecorder;
    private final ApplicationEventPublisher events;

    public PasswordRecoveryService(PasswordRecoveryProperties properties, RecoveryRateLimiter limiter,
            @Qualifier("recoveryMailExecutor") TaskExecutor executor, RecoveryMailDelivery delivery,
            PasswordRecoveryMapper mapper, UserMapper userMapper, PasswordEncoder encoder,
            AuditRecorder auditRecorder, ApplicationEventPublisher events) {
        this.properties = properties;
        this.limiter = limiter;
        this.executor = executor;
        this.delivery = delivery;
        this.mapper = mapper;
        this.userMapper = userMapper;
        this.encoder = encoder;
        this.auditRecorder = auditRecorder;
        this.events = events;
    }

    public boolean enabled() { return properties.enabled(); }

    public void request(String email, String remoteAddress) {
        requireEnabled();
        if (!limiter.allow("send-global", "all", 500, 3600)
                || !limiter.allow("send-ip", remoteAddress, 20, 900)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
        // A throttled email gets the same response as an unknown or disabled account.
        if (!limiter.allow("send-email", email, 5, 3600)
                || !limiter.allowEmailCooldown(email)) return;
        try {
            executor.execute(() -> delivery.deliver(email));
        } catch (TaskRejectedException exception) {
            throw new BusinessException(ErrorCode.RECOVERY_UNAVAILABLE);
        }
    }

    // Called before the reset transaction so rejected/invalid attempts still consume their quota.
    public void checkResetLimit(String remoteAddress) {
        requireEnabled();
        if (!limiter.allow("reset-global", "all", 3000, 3600)
                || !limiter.allow("reset-ip", remoteAddress, 30, 900)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void reset(ResetPasswordCommand command) {
        requireEnabled();
        validatePassword(command.newPassword());
        if (command.token() == null || !command.token().matches("[A-Za-z0-9_-]{43}")) {
            throw new BusinessException(ErrorCode.INVALID_RESET_TOKEN);
        }
        String hash = RecoveryTokens.hash(command.token());
        PasswordRecoveryMapper.Token candidate = mapper.findToken(hash);
        if (candidate == null) throw new BusinessException(ErrorCode.INVALID_RESET_TOKEN);
        // Lock the parent user before the token, matching FK/upsert lock ordering.
        UserEntity user = userMapper.selectRecoveryUserForUpdate(candidate.userId());
        PasswordRecoveryMapper.Token token = mapper.lockToken(hash);
        if (token == null || !token.expiresAt().isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.INVALID_RESET_TOKEN);
        }
        if (user == null || user.getStatus() != UserStatus.ACTIVE
                || !Objects.equals(user.getEmail(), token.email())
                || !RecoveryTokens.hash(user.getPasswordHash()).equals(token.passwordFingerprint())) {
            throw new BusinessException(ErrorCode.INVALID_RESET_TOKEN);
        }
        if (encoder.matches(command.newPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "新密码不能与原密码相同");
        }
        user.setPasswordHash(encoder.encode(command.newPassword()));
        user.setUpdatedAt(Instant.now());
        if (userMapper.resetRecoveredPassword(user) != 1 || mapper.deleteToken(hash) != 1) {
            throw new BusinessException(ErrorCode.DATA_VERSION_CONFLICT);
        }
        auditRecorder.record(user.getId(), AuditActions.PASSWORD_RESET, "USER", user.getId().toString(),
                null, Map.of("channel", "EMAIL", "allSessionsInvalidated", true));
        events.publishEvent(new PasswordChangedEvent(user.getAccount(), null));
        events.publishEvent(new PasswordResetNotification.Completed(user.getEmail()));
    }

    private void requireEnabled() {
        if (!enabled()) throw new BusinessException(ErrorCode.RECOVERY_UNAVAILABLE);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 72
                || password.getBytes(StandardCharsets.UTF_8).length > 72
                || !password.matches("(?s).*[A-Za-z].*") || !password.matches("(?s).*\\d.*")) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT,
                    "新密码需为 8～72 个字符、包含字母和数字，且 UTF-8 编码不超过 72 字节");
        }
    }
}
