package cn.edu.techgroup.outsourcing.modules.user.service.impl;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditActions;
import cn.edu.techgroup.outsourcing.modules.audit.service.AuditRecorder;
import cn.edu.techgroup.outsourcing.modules.user.dto.ChangePasswordCommand;
import cn.edu.techgroup.outsourcing.modules.user.dto.UpdateMyProfileCommand;
import cn.edu.techgroup.outsourcing.modules.user.entity.UserEntity;
import cn.edu.techgroup.outsourcing.modules.user.event.PasswordChangedEvent;
import cn.edu.techgroup.outsourcing.modules.user.mapper.UserMapper;
import cn.edu.techgroup.outsourcing.modules.user.service.UserProfileService;
import cn.edu.techgroup.outsourcing.modules.user.vo.PasswordChangeResultVO;
import cn.edu.techgroup.outsourcing.modules.user.vo.UserProfileVO;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditRecorder auditRecorder;
    private final ApplicationEventPublisher eventPublisher;

    public UserProfileServiceImpl(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            AuditRecorder auditRecorder,
            ApplicationEventPublisher eventPublisher) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditRecorder = auditRecorder;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileVO get(LoginUser operator) {
        return UserProfileVO.from(findCurrentUser(operator));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO update(
            UpdateMyProfileCommand command,
            LoginUser operator) {
        if (command == null || command.displayName() == null) {
            throw invalidArgument("个人资料参数不正确");
        }
        UserEntity user = findCurrentUser(operator);
        Map<String, Object> before = snapshot(user);
        user.setDisplayName(command.displayName());
        user.setEmail(command.email() == null
                ? null
                : command.email().toLowerCase(Locale.ROOT));
        user.setPhone(command.phone());
        user.setDepartment(command.department());
        user.setUpdatedAt(Instant.now());
        try {
            if (userMapper.updateSelfProfile(user) != 1) {
                throw new BusinessException(ErrorCode.DATA_VERSION_CONFLICT);
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "邮箱已被其他账号使用");
        }
        auditRecorder.record(
                operator.id(),
                AuditActions.PROFILE_UPDATED,
                "USER",
                operator.id().toString(),
                before,
                snapshot(user));
        return UserProfileVO.from(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PasswordChangeResultVO changePassword(
            ChangePasswordCommand command,
            String currentSessionId,
            LoginUser operator) {
        if (command == null) {
            throw invalidArgument("密码参数不正确");
        }
        UserEntity user = findCurrentUser(operator);
        if (!passwordEncoder.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "当前密码不正确");
        }
        validateNewPassword(command.newPassword());
        if (passwordEncoder.matches(command.newPassword(), user.getPasswordHash())) {
            throw invalidArgument("新密码不能与当前密码相同");
        }

        user.setPasswordHash(passwordEncoder.encode(command.newPassword()));
        user.setUpdatedAt(Instant.now());
        if (userMapper.updatePasswordHash(user) != 1) {
            throw new BusinessException(ErrorCode.DATA_VERSION_CONFLICT);
        }
        auditRecorder.record(
                operator.id(),
                AuditActions.PASSWORD_CHANGED,
                "USER",
                operator.id().toString(),
                null,
                Map.of("otherSessionsInvalidated", true));
        eventPublisher.publishEvent(new PasswordChangedEvent(user.getAccount(), currentSessionId));
        return new PasswordChangeResultVO(true);
    }

    private UserEntity findCurrentUser(LoginUser operator) {
        if (operator == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        UserEntity user = userMapper.selectById(operator.id());
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        return user;
    }

    private void validateNewPassword(String password) {
        if (password == null
                || password.length() < 8
                || password.length() > MAX_BCRYPT_PASSWORD_BYTES
                || password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES
                || !password.matches(".*[A-Za-z].*")
                || !password.matches(".*\\d.*")) {
            throw invalidArgument(
                    "新密码需为 8～72 个字符、包含字母和数字，且 UTF-8 编码不超过 72 字节");
        }
    }

    private Map<String, Object> snapshot(UserEntity user) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("displayName", user.getDisplayName());
        snapshot.put("email", user.getEmail());
        snapshot.put("phone", user.getPhone());
        snapshot.put("department", user.getDepartment());
        return snapshot;
    }

    private BusinessException invalidArgument(String message) {
        return new BusinessException(ErrorCode.INVALID_ARGUMENT, message);
    }
}
