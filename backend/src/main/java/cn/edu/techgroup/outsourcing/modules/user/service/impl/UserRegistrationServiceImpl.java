package cn.edu.techgroup.outsourcing.modules.user.service.impl;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.config.RegistrationProperties;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditActions;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.user.dto.RegisterUserCommand;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserStatus;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.modules.user.service.UserRegistrationService;
import cn.edu.techgroup.outsourcing.modules.user.vo.RegistrationStatusVO;
import cn.edu.techgroup.outsourcing.modules.user.vo.UserProfileVO;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserRegistrationServiceImpl implements UserRegistrationService {

    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;

    private final RegistrationProperties properties;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditRecorder auditRecorder;

    public UserRegistrationServiceImpl(
            RegistrationProperties properties,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            AuditRecorder auditRecorder) {
        this.properties = properties;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public RegistrationStatusVO status() {
        return new RegistrationStatusVO(
                properties.enabled(),
                normalizeSuffix(properties.emailSuffix()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO register(RegisterUserCommand command) {
        if (!properties.enabled()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "需求方自助注册未开放");
        }
        if (command == null
                || command.account() == null
                || command.password() == null
                || command.displayName() == null
                || command.email() == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT);
        }
        validatePassword(command.password());
        String email = command.email().toLowerCase(Locale.ROOT);
        String suffix = normalizeSuffix(properties.emailSuffix());
        if (suffix != null && !email.endsWith(suffix)) {
            throw new BusinessException(
                    ErrorCode.INVALID_ARGUMENT,
                    "请使用 " + suffix + " 后缀的邮箱注册");
        }

        UserEntity user = new UserEntity();
        user.setAccount(command.account().toLowerCase(Locale.ROOT));
        user.setPasswordHash(passwordEncoder.encode(command.password()));
        user.setDisplayName(command.displayName());
        user.setEmail(email);
        user.setPhone(command.phone());
        user.setDepartment(command.department());
        user.setRole(UserRole.REQUESTER);
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginCount(0);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(user.getCreatedAt());
        try {
            if (userMapper.insert(user) != 1 || user.getId() == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "账号或邮箱已存在");
        }

        auditRecorder.record(
                user.getId(),
                AuditActions.USER_REGISTERED,
                "USER",
                user.getId().toString(),
                null,
                Map.of("role", UserRole.REQUESTER, "status", UserStatus.ACTIVE));
        return UserProfileVO.from(user);
    }

    private void validatePassword(String password) {
        if (password == null
                || password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            throw new BusinessException(
                    ErrorCode.INVALID_ARGUMENT,
                    "密码 UTF-8 编码不能超过 72 字节");
        }
    }

    private String normalizeSuffix(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("@") ? normalized : "@" + normalized;
    }
}
